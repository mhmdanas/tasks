/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.adapter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.todoroo.andlib.data.Property;
import com.todoroo.andlib.data.Property.StringProperty;
import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.actfm.sync.messages.NameMaps;
import com.todoroo.astrid.data.UserActivity;

import org.tasks.R;

/**
 * Adapter for displaying a user's activity
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class UpdateAdapter extends CursorAdapter {

    // --- instance variables

    protected final Fragment fragment;
    private final int resource;
    private final LayoutInflater inflater;

    public static final StringProperty ACTIVITY_TYPE_PROPERTY = new StringProperty(null, "'" + NameMaps.TABLE_ID_USER_ACTIVITY + "' as type");  //$NON-NLS-1$//$NON-NLS-2$

    public static final Property<?>[] USER_ACTIVITY_PROPERTIES = {
        UserActivity.CREATED_AT,
        UserActivity.UUID,
        UserActivity.ACTION,
        UserActivity.MESSAGE,
        UserActivity.TARGET_ID,
        UserActivity.TARGET_NAME,
        UserActivity.PICTURE,
        UserActivity.USER_UUID,
        UserActivity.ID,
        ACTIVITY_TYPE_PROPERTY,
    };

    public static final int TYPE_PROPERTY_INDEX = USER_ACTIVITY_PROPERTIES.length - 1;

    private final int color;

    /**
     * Constructor
     *
     * @param resource
     *            layout resource to inflate
     * @param c
     *            database cursor
     */
    public UpdateAdapter(Fragment fragment, int resource,
            Cursor c) {
        super(fragment.getActivity(), c, false);

        inflater = (LayoutInflater) fragment.getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        this.resource = resource;
        this.fragment = fragment;

        TypedValue tv = new TypedValue();
        fragment.getActivity().getTheme().resolveAttribute(R.attr.asTextColor, tv, false);
        color = tv.data;

        fragment.getActivity().getTheme().resolveAttribute(R.attr.asDueDateColor, tv, false);
    }

    /* ======================================================================
     * =========================================================== view setup
     * ====================================================================== */

    private class ModelHolder {
        public UserActivity activity = new UserActivity();
    }

    /** Creates a new view for use in the list view */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        ViewGroup view = (ViewGroup)inflater.inflate(resource, parent, false);

        view.setTag(new ModelHolder());

        // populate view content
        bindView(view, context, cursor);

        return view;
    }

    /** Populates a view with content */
    @Override
    public void bindView(View view, Context context, Cursor c) {
        TodorooCursor<UserActivity> cursor = (TodorooCursor<UserActivity>)c;
        ModelHolder mh = ((ModelHolder) view.getTag());

        String type = cursor.getString(TYPE_PROPERTY_INDEX);

        UserActivity update = mh.activity;
        update.clear();

        readUserActivityProperties(cursor, update);

        setFieldContentsAndVisibility(view, update, type);
    }

    public static void readUserActivityProperties(TodorooCursor<UserActivity> unionCursor, UserActivity activity) {
        activity.setCreatedAt(unionCursor.getLong(0));
        activity.setUUID(unionCursor.getString(1));
        activity.setAction(unionCursor.getString(2));
        activity.setMessage(unionCursor.getString(3));
        activity.setTargetId(unionCursor.getString(4));
        activity.setTargetName(unionCursor.getString(5));
        activity.setPicture(unionCursor.getString(6));
        activity.setUserUUID(unionCursor.getString(7));
    }

    /** Helper method to set the contents and visibility of each field */
    public synchronized void setFieldContentsAndVisibility(View view, UserActivity activity, String type) {
        // picture
        if (NameMaps.TABLE_ID_USER_ACTIVITY.equals(type)) {
            setupUserActivityRow(view, activity);
        }
    }

    private void setupUserActivityRow(View view, UserActivity activity) {
        final ImageView commentPictureView = (ImageView)view.findViewById(R.id.comment_picture); {
            Uri updateBitmap = activity.getPictureUri();
            setupImagePopupForCommentView(view, commentPictureView, updateBitmap, fragment);
        }

        // name
        final TextView nameView = (TextView)view.findViewById(R.id.title); {
            nameView.setText(getUpdateComment(activity));
            nameView.setMovementMethod(new LinkMovementMethod());
            nameView.setTextColor(color);
        }

        // date
        final TextView date = (TextView)view.findViewById(R.id.date); {
            CharSequence dateString = DateUtils.getRelativeTimeSpanString(activity.getCreatedAt(),
                    DateUtilities.now(), DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);
            date.setText(dateString);
        }
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

    public static void setupImagePopupForCommentView(View view, ImageView commentPictureView, final Uri updateBitmap,
            final Fragment fragment) {
        if (updateBitmap != null) { //$NON-NLS-1$
            commentPictureView.setVisibility(View.VISIBLE);
            commentPictureView.setImageURI(updateBitmap);

            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    fragment.startActivity(new Intent(Intent.ACTION_VIEW) {{
                        setDataAndType(updateBitmap, "image/jpg");
                    }});
                }
            });
        } else {
            commentPictureView.setVisibility(View.GONE);
        }
    }

    public static Spanned getUpdateComment(UserActivity activity) {
        String message = activity.getMessage();
        return Html.fromHtml(message);
    }
}
