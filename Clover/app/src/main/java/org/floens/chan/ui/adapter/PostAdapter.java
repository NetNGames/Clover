/*
 * Clover - 4chan browser https://github.com/Floens/Clover/
 * Copyright (C) 2014  Floens
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.floens.chan.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.floens.chan.R;
import org.floens.chan.core.model.ChanThread;
import org.floens.chan.core.model.Loadable;
import org.floens.chan.core.model.Post;
import org.floens.chan.ui.cell.PostCellInterface;
import org.floens.chan.ui.cell.ThreadStatusCell;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_POST = 0;
    private static final int TYPE_STATUS = 1;

    private final PostAdapterCallback postAdapterCallback;
    private final PostCellInterface.PostCellCallback postCellCallback;
    private final ThreadStatusCell.Callback statusCellCallback;
    private RecyclerView recyclerView;

    private final List<Post> sourceList = new ArrayList<>();
    private final List<Post> displayList = new ArrayList<>();
    private int lastPostCount = 0;
    private String error = null;
    private Post highlightedPost;
    private String highlightedPostId;
    private int highlightedPostNo = -1;
    private boolean filtering = false;
    private PostCellInterface.PostViewMode postViewMode;

    public PostAdapter(RecyclerView recyclerView, PostAdapterCallback postAdapterCallback, PostCellInterface.PostCellCallback postCellCallback, ThreadStatusCell.Callback statusCellCallback) {
        this.recyclerView = recyclerView;
        this.postAdapterCallback = postAdapterCallback;
        this.postCellCallback = postCellCallback;
        this.statusCellCallback = statusCellCallback;

        setHasStableIds(true);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_POST) {
            int layout = 0;
            switch (postViewMode) {
                case LIST:
                    layout = R.layout.cell_post;
                    break;
                case CARD:
                    layout = R.layout.cell_post_card;
                    break;
            }

            PostCellInterface postCell = (PostCellInterface) LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            return new PostViewHolder(postCell);
        } else {
            StatusViewHolder statusViewHolder = new StatusViewHolder((ThreadStatusCell) LayoutInflater.from(parent.getContext()).inflate(R.layout.cell_thread_status, parent, false));
            statusViewHolder.threadStatusCell.setCallback(statusCellCallback);
            statusViewHolder.threadStatusCell.setError(error);
            return statusViewHolder;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_POST) {
            PostViewHolder postViewHolder = (PostViewHolder) holder;
            Post post = displayList.get(position);
            boolean highlight = post == highlightedPost || post.id.equals(highlightedPostId) || post.no == highlightedPostNo;
            postViewHolder.postView.setPost(null, post, postCellCallback, highlight, -1);
        } else if (getItemViewType(position) == TYPE_STATUS) {
            ((StatusViewHolder) holder).threadStatusCell.update();
            onScrolledToBottom();
        }
    }

    @Override
    public int getItemCount() {
        if (showStatusView()) {
            return displayList.size() + 1;
        } else {
            return displayList.size();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (showStatusView()) {
            if (position == getItemCount() - 1) {
                return TYPE_STATUS;
            } else {
                return TYPE_POST;
            }
        } else {
            return TYPE_POST;
        }
    }

    @Override
    public long getItemId(int position) {
        if (getItemViewType(position) != TYPE_POST) {
            return -1;
        } else {
            return displayList.get(position).no;
        }
    }

    public void setThread(ChanThread thread) {
        showError(null);
        sourceList.clear();
        sourceList.addAll(thread.posts);

        if (!filtering) {
            displayList.clear();
            displayList.addAll(sourceList);
        }

        // Update all, recyclerview will figure out all the animations
        notifyDataSetChanged();
    }

    public void cleanup() {
        highlightedPost = null;
        highlightedPostId = null;
        highlightedPostNo = -1;
        filtering = false;
        lastPostCount = 0;
    }

    public void showError(String error) {
        this.error = error;
        if (showStatusView()) {
            RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(getItemCount() - 1);
            // Recyclerview did not sync yet
            if (viewHolder instanceof StatusViewHolder) {
                ThreadStatusCell threadStatusCell = ((StatusViewHolder) viewHolder).threadStatusCell;
                threadStatusCell.setError(error);
                threadStatusCell.update();
            }
        }
    }

    public void filterList(List<Post> filter) {
        filtering = true;

        displayList.clear();
        for (Post item : sourceList) {
            for (Post filterItem : filter) {
                if (filterItem.no == item.no) {
                    displayList.add(item);
                    break;
                }
            }
        }

        notifyDataSetChanged();
    }

    public void clearFilter() {
        if (filtering) {
            filtering = false;

            displayList.clear();
            displayList.addAll(sourceList);

            notifyDataSetChanged();
        }
    }

    public void highlightPost(Post post) {
        highlightedPost = post;
        highlightedPostId = null;
        highlightedPostNo = -1;
        notifyDataSetChanged();
    }

    public void highlightPostId(String id) {
        highlightedPost = null;
        highlightedPostId = id;
        highlightedPostNo = -1;
        notifyDataSetChanged();
    }

    public void highlightPostNo(int no) {
        highlightedPost = null;
        highlightedPostId = null;
        highlightedPostNo = no;
        notifyDataSetChanged();
    }

    private void onScrolledToBottom() {
        if (!filtering && lastPostCount != sourceList.size()) {
            lastPostCount = sourceList.size();
            postAdapterCallback.onListScrolledToBottom();
        }
    }

    private boolean showStatusView() {
        return postAdapterCallback.getLoadable().isThreadMode();
    }

    public void setPostViewMode(PostCellInterface.PostViewMode postViewMode) {
        this.postViewMode = postViewMode;
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        private PostCellInterface postView;

        public PostViewHolder(PostCellInterface postView) {
            super((View) postView);
            this.postView = postView;
        }
    }

    public static class StatusViewHolder extends RecyclerView.ViewHolder {
        private ThreadStatusCell threadStatusCell;

        public StatusViewHolder(ThreadStatusCell threadStatusCell) {
            super(threadStatusCell);
            this.threadStatusCell = threadStatusCell;
        }
    }

    public interface PostAdapterCallback {
        Loadable getLoadable();

        void onListScrolledToBottom();
    }
}
