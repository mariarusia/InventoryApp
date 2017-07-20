package com.example.android.inventoryapp.data;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.inventoryapp.R;

import static android.content.ContentValues.TAG;

/**
 * {@link BookCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of book data as its data source. This adapter knows
 * how to create list items for each row of book data in the {@link Cursor}.
 */
public class BookCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link BookCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public BookCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the  data (in the current row pointed to by cursor) to the given
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView priceTextView = (TextView) view.findViewById(R.id.textfield_price);
        TextView quantityTextView = (TextView) view.findViewById(R.id.textfield_quantity);
        Button buyButton = (Button) view.findViewById(R.id.button_buy);

        // Find the columns with attributes
        final int bookId = cursor.getInt(cursor.getColumnIndex(BookContract.BookEntry._ID));
        int nameColumnIndex = cursor.getColumnIndex(BookContract.BookEntry.COLUMN_BOOK_NAME);
        int priceColumnIndex = cursor.getColumnIndex(BookContract.BookEntry.COLUMN_BOOK_PRICE);
        final int quantityColumnIndex = cursor.getColumnIndex(BookContract.BookEntry.COLUMN_BOOK_QUANTITY);

        // Read the book attributes from the Cursor for the current book
        String bookName = cursor.getString(nameColumnIndex);
        int bookPrice = cursor.getInt(priceColumnIndex);
        final int quantity = cursor.getInt(quantityColumnIndex);

        buyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //int quantity = cursor.getInt(quantityColumnIndex);
                int result_quantity = reduceByOne(quantity);
                Log.v("quantity", String.valueOf(quantity));
                Log.v("id", "" + bookId);

                Uri productUri = ContentUris.withAppendedId(BookContract.BookEntry.CONTENT_URI, bookId);
                refresh(context, productUri, result_quantity);
            }
        });

        // Update the TextViews with the attributes for the current book
        nameTextView.setText(bookName);
        priceTextView.setText("Price:    " + bookPrice);
        quantityTextView.setText("Quantity: " + quantity);
    }

    private void refresh(Context context, Uri productUri, int quantity) {

        // Update table with new stock of the product
        ContentValues contentValues = new ContentValues();
        contentValues.put(BookContract.BookEntry.COLUMN_BOOK_QUANTITY, quantity);
        int numRowsUpdated = context.getContentResolver().update(productUri, contentValues, null, null);

        // Display error message in Log if product stock fails to update
        if (!(numRowsUpdated > 0)) {
            Log.e(TAG, context.getString(R.string.error_update));
        }
    }

    public int reduceByOne(int quantity) {
        if (quantity > 0) {
            quantity -= 1;
        }
        return quantity;
    }
}