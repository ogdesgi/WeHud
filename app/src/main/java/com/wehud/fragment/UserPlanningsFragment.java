package com.wehud.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.reflect.TypeToken;
import com.wehud.R;
import com.wehud.adapter.PlanningsAdapter;
import com.wehud.dialog.EditDialogFragment;
import com.wehud.dialog.TextDialogFragment;
import com.wehud.model.Payload;
import com.wehud.model.Planning;
import com.wehud.model.Reward;
import com.wehud.network.APICall;
import com.wehud.util.Constants;
import com.wehud.util.GsonUtils;
import com.wehud.util.PreferencesUtils;
import com.wehud.util.Utils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserPlanningsFragment extends Fragment
        implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener,
        EditDialogFragment.OnEditDialogDismissOkListener,
        TextDialogFragment.OnTextDialogDismissOkListener {

    private static final String KEY_USER_ID = "key_user_id";
    private static final String KEY_PLANNINGS = "key_plannings";

    private static final String PARAM_TITLE = "title";

    private Context mContext;

    private View mEmptyLayout;
    private ViewGroup mListLayout;
    private SwipeRefreshLayout mSwipeLayout;
    private RecyclerView mPlanningListView;

    private FragmentManager mManager;
    private String mCurrentUserId;
    private List<Planning> mPlannings;

    private boolean mPaused;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mPaused) {
                final String response = intent.getStringExtra(Constants.EXTRA_BROADCAST);
                final Payload payload = GsonUtils.getInstance().fromJson(response, Payload.class);

                final String code = payload.getCode();

                if (Integer.valueOf(code) == Constants.HTTP_OK ||
                        Integer.valueOf(code) == Constants.HTTP_CREATED ||
                        Integer.valueOf(code) == Constants.HTTP_NO_CONTENT) {
                    final String content = payload.getContent();

                    switch (intent.getAction()) {
                        case Constants.INTENT_PLANNINGS_ADD:
                            final Reward reward = Utils.getNestedReward(content);
                            if (Utils.isNotEmpty(reward.getEntities())) {
                                Utils.generateRewardDialog(
                                        mContext,
                                        getFragmentManager(),
                                        UserPlanningsFragment.this,
                                        reward,
                                        0
                                );
                            } else {
                                Utils.toast(mContext, R.string.message_addPlanningSuccess);
                                mSwipeLayout.setRefreshing(true);
                                getPlannings();
                            }
                            break;
                        case Constants.INTENT_PLANNINGS_DELETE:
                            Utils.toast(mContext, R.string.message_deletePlanningSuccess);
                            mSwipeLayout.setRefreshing(true);
                            getPlannings();
                            break;
                        case Constants.INTENT_PLANNINGS_LIST:
                            final Type planningListType = new TypeToken<List<Planning>>(){}
                                    .getType();
                            mPlannings = GsonUtils.getInstance().fromJson(
                                    content, planningListType
                            );

                            if (Utils.isNotEmpty(mPlannings)) {
                                PlanningsAdapter adapter = new PlanningsAdapter(mPlannings);
                                adapter.setViewResourceId(1);
                                adapter.setFragmentManager(mManager);
                                mPlanningListView.setLayoutManager(
                                        new LinearLayoutManager(mContext)
                                );
                                mPlanningListView.setAdapter(adapter);

                                mEmptyLayout.setVisibility(View.GONE);
                                mListLayout.setVisibility(View.VISIBLE);
                            } else {
                                mEmptyLayout.setVisibility(View.VISIBLE);
                                mListLayout.setVisibility(View.GONE);
                            }

                            mSwipeLayout.setRefreshing(false);
                            break;
                        case Constants.INTENT_PLANNINGS_UNBIND:
                            Utils.toast(mContext, R.string.message_unboundPlanning);
                            getPlannings();
                            break;
                        default:
                            break;
                    }
                } else {
                    int messageId;
                    switch (Integer.valueOf(code)) {
                        case Constants.HTTP_UNAUTHORIZED:
                            messageId = R.string.error_sessionExpired;
                            getActivity().finish();
                            break;
                        case Constants.HTTP_INTERNAL_SERVER_ERROR:
                            messageId = R.string.error_server;
                            break;
                        default:
                            Utils.toast(mContext, R.string.error_general, code);
                            return;
                    }

                    Utils.toast(mContext, messageId);
                }
            }
        }
    };

    public static Fragment newInstance(String userId) {
        Fragment fragment = new UserPlanningsFragment();
        Bundle args = new Bundle();
        args.putString(KEY_USER_ID, userId);
        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mCurrentUserId = savedInstanceState.getString(KEY_USER_ID);
            mPlannings = savedInstanceState.getParcelableArrayList(KEY_PLANNINGS);
        } else {
            Bundle args = getArguments();
            if (args != null) mCurrentUserId = args.getString(KEY_USER_ID);
        }

        final View view = inflater.inflate(R.layout.fragment_user_plannings, container, false);
        mContext = view.getContext();
        mManager = getFragmentManager();

        mEmptyLayout = view.findViewById(R.id.layout_empty);
        mListLayout = (ViewGroup) view.findViewById(R.id.layout_list);
        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.layout_swipe);
        mPlanningListView = (RecyclerView) view.findViewById(android.R.id.list);

        mEmptyLayout.setVisibility(View.VISIBLE);
        mListLayout.setVisibility(View.GONE);

        mSwipeLayout.setOnRefreshListener(this);
        mSwipeLayout.setRefreshing(true);

        TextView noPlannings = (TextView) view.findViewById(R.id.no_plannings);
        Button createPlanningButton = (Button) view.findViewById(R.id.btnCreatePlanning);
        Button createFirstPlanningButton = (Button) view.findViewById(R.id.btnCreateFirstPlanning);
        createPlanningButton.setOnClickListener(this);
        createFirstPlanningButton.setOnClickListener(this);

        final boolean isConnectedUser = Utils.isConnectedUser(
                mContext,
                mCurrentUserId
        );
        if (!isConnectedUser) {
            noPlannings.setText(getString(R.string.no_plannings));
            createFirstPlanningButton.setVisibility(View.GONE);
            createPlanningButton.setVisibility(View.GONE);
        } else noPlannings.setText(getString(R.string.no_connected_plannings));

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.INTENT_PLANNINGS_LIST);
        filter.addAction(Constants.INTENT_PLANNINGS_ADD);
        filter.addAction(Constants.INTENT_PLANNINGS_DELETE);
        filter.addAction(Constants.INTENT_PLANNINGS_UNBIND);

        mContext.registerReceiver(mReceiver, filter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!TextUtils.isEmpty(mCurrentUserId)) this.getPlannings();

        mPaused = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        mPaused = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_USER_ID, mCurrentUserId);
        outState.putParcelableArrayList(KEY_PLANNINGS, (ArrayList<Planning>) mPlannings);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnCreatePlanning:
            case R.id.btnCreateFirstPlanning:
                EditDialogFragment.generate(
                        mManager,
                        this,
                        getString(R.string.dialogTitle_createPlanning),
                        null,
                        getString(R.string.hint_planningTitle),
                        false,
                        0
                );
                break;
            default:
                break;
        }
    }

    @Override
    public void onRefresh() {
        if (!TextUtils.isEmpty(mCurrentUserId)) this.getPlannings();
    }

    @Override
    public void onEditDialogDismissOk(Object id, String text) {
        if (!TextUtils.isEmpty(text)) this.createPlanning(text);
    }

    @Override
    public void onTextDialogDismissOk(Object o) {
        Utils.toast(mContext, R.string.message_addPlanningSuccess);
        this.getPlannings();
    }

    private void getPlannings() {
        if (!TextUtils.isEmpty(mCurrentUserId)) {
            Map<String, String> headers = new HashMap<>();
            headers.put(Constants.HEADER_CONTENT_TYPE, Constants.APPLICATION_JSON);
            headers.put(Constants.HEADER_ACCEPT, Constants.APPLICATION_JSON);

            final APICall call = new APICall(
                    mContext,
                    Constants.INTENT_PLANNINGS_LIST,
                    Constants.GET,
                    Constants.API_USERS_PLANNINGS + '/' + mCurrentUserId,
                    headers
            );
            if (!call.isLoading()) call.execute();
        }
    }

    private void createPlanning(String title) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HEADER_CONTENT_TYPE, Constants.APPLICATION_JSON);
        headers.put(Constants.HEADER_ACCEPT, Constants.APPLICATION_JSON);

        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.PARAM_TOKEN, PreferencesUtils.get(mContext, Constants.PREF_TOKEN));

        Map<String, String> planning = new HashMap<>();
        planning.put(PARAM_TITLE, title);

        final String body = GsonUtils.getInstance().toJson(planning);

        final APICall call = new APICall(
                mContext,
                Constants.INTENT_PLANNINGS_ADD,
                Constants.POST,
                Constants.API_PLANNINGS,
                body,
                headers,
                parameters
        );
        if (!call.isLoading()) call.execute();
    }
}
