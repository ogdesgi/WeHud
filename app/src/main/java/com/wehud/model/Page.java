package com.wehud.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public final class Page implements Parcelable {

    @SerializedName("_id")
    private String mId;

    @SerializedName("title")
    private String mTitle;

    @SerializedName("mOwner")
    private User mOwner;

    @SerializedName("users")
    private List<String> mUsers;

    @SerializedName("games")
    private List<String> mGames;

    @SerializedName("posts")
    private List<Post> mPosts;

    public String getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public User getOwner() {
        return mOwner;
    }

    public List<String> getUsers() {
        return mUsers;
    }

    public List<String> getGames() {
        return mGames;
    }

    public List<Post> getPosts() {
        return mPosts;
    }

    protected Page(Parcel in) {
        mId = in.readString();
        mTitle = in.readString();
        mOwner = in.readParcelable(User.class.getClassLoader());
        mUsers = in.createStringArrayList();
        mGames = in.createStringArrayList();
        mPosts = in.createTypedArrayList(Post.CREATOR);
    }

    public static final Creator<Page> CREATOR = new Creator<Page>() {
        @Override
        public Page createFromParcel(Parcel in) {
            return new Page(in);
        }

        @Override
        public Page[] newArray(int size) {
            return new Page[size];
        }
    };

    @Override
    public String toString() {
        return "Page{" +
                "mId='" + mId + '\'' +
                ", mTitle='" + mTitle + '\'' +
                ", mOwner=" + mOwner +
                ", mUsers=" + mUsers +
                ", mGames=" + mGames +
                ", mPosts=" + mPosts +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mId);
        parcel.writeString(mTitle);
        parcel.writeParcelable(mOwner, i);
        parcel.writeStringList(mUsers);
        parcel.writeStringList(mGames);
        parcel.writeTypedList(mPosts);
    }
}
