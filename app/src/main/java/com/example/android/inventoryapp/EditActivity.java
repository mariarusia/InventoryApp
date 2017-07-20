package com.example.android.inventoryapp;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.BookContract;

import static com.example.android.inventoryapp.data.BookProvider.LOG_TAG;

public class EditActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Identifier for the book data loader
     */
    private static final int EXISTING_BOOK_LOADER = 0;

    //identifier for the picture ImageVies
    private ImageView mImageBook;

    //the String we are going to use to save the pic in case the
    private static final String STATE_IMAGE_URI = "STATE_IMAGE_URI";

    /**
     * Content URI for the existing book (null if it's a new book)
     */
    private Uri mCurrentBookUri;

    /**
     * EditText field to enter the book's name
     */
    private EditText mNameEditText;

    //the URI for a pic on the phone to load
    private Uri mImageUri;

    /**
     * EditText field to enter the price of the book
     */
    private EditText mPriceEditText;

    /**
     * EditText field to enter the quantity of book
     */
    private EditText mQuantityEditText;

    /**
     * Boolean flag that keeps track of whether the book has been edited (true) or not (false)
     */
    private boolean mBookHasChanged = false;

    private String name;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mBookHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mBookHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new book or editing an existing one.
        Intent intent = getIntent();
        mCurrentBookUri = intent.getData();

        // If the intent DOES NOT contain a book content URI, then we know that we are
        // creating a new book.
        if (mCurrentBookUri == null) {
            // This is a new book, so change the app bar to say "New Book"
            setTitle(getString(R.string.editor_activity_title_new_book));
        } else {
            // Otherwise this is an existing book, so change app bar to say "Details"
            setTitle(getString(R.string.editor_activity_title_details));

            // Initialize a loader to read the book data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_BOOK_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_name);
        mPriceEditText = (EditText) findViewById(R.id.edit_price);
        mQuantityEditText = (EditText) findViewById(R.id.edit_quantity);
        mImageBook = (ImageView) findViewById(R.id.imageBook);

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);

        //delete the item after clicking the delete button
        Button deleteItem = (Button) findViewById(R.id.delete_item);

        deleteItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmationDialog();
            }
        });


        //handling the situation when + is pressed
        Button addItem = (Button) findViewById(R.id.button_plus);
        addItem.setOnTouchListener(mTouchListener);
        addItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int quantity = addOne();

                mQuantityEditText.setText(String.valueOf(quantity));
            }
        });

        //handling the situation when - is pressed
        Button deleteButton = (Button) findViewById(R.id.button_minus);
        deleteButton.setOnTouchListener(mTouchListener);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int quantity = decreaseByOne();

                mQuantityEditText.setText(String.valueOf(quantity));
            }
        });

        //handling the situation when order is pressed. Let`s say, we open an intent to write to the provider
        Button orderButton = (Button) findViewById(R.id.button_order);
        orderButton.setOnTouchListener(mTouchListener);
        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/html");
                Log.v(LOG_TAG, "subject" + name);

                name = mNameEditText.getText().toString();
                if (!name.isEmpty()) {

                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.order_subject) + name);
                    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.order_body) + name);

                }else{
                    //in case there is no name we`ll leave the book blank
                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.order_subject));
                    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.order_body));
                }

                startActivity(Intent.createChooser(intent, "Send Email"));
            }
        });

        //handling the situation when a pic is loaded
        Button addPic = (Button) findViewById(R.id.button_pic);

        addPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent in = new Intent(
                        Intent.ACTION_OPEN_DOCUMENT,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(in, 1); //the code that we use to know that the pic was laded

            }

        });

    }

    //if the pic is loaded, set it to the imageView
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && (resultCode == RESULT_OK)) {

            mImageUri = data.getData();

            mImageBook.setImageURI(mImageUri);
            Log.v(LOG_TAG, "Trying to set uri");
        }
    }

    /**
     * a method to add one book to the stock
     */
    private int addOne() {
        String quantity = mQuantityEditText.getText().toString().trim();

        //check if the user entered smth that is not a number
        try {
            int number = Integer.valueOf(quantity);

            number = number + 1;

            //check if the resulting value is > 0 ;
            if (number > 0) {
                return number;
            } else {
                return 0;
            }
            //catching the situation when the user entered smth that is not a number and tries to increase/decrease it
        } catch (java.lang.NumberFormatException e) {
            return 0;
        }
    }

    /**
     * a method to remove one book to the stock
     */
    private int decreaseByOne() {
        String quantity = mQuantityEditText.getText().toString().trim();

        //check if the user entered smth that is not a number
        try {
            int number = Integer.valueOf(quantity);

            if (number > 0) {
                return number - 1;
            } else {
                return 0;
            }
            //catching the situation when the user entered smth that is not a number and tries to increase/decrease it
        } catch (java.lang.NumberFormatException e) {
            return 0;
        }

    }

    //saving the book
    private void saveBook() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String price = mPriceEditText.getText().toString().trim();
        String quantity = mQuantityEditText.getText().toString().trim();

        // Check if this is supposed to be a new book
        // and check if the name field is blank

        if (mCurrentBookUri == null &&
                TextUtils.isEmpty(nameString)) {
            // Since no fields were modified, we can return early without creating a new book.
            // No need to create ContentValues and no need to do any ContentProvider operations.

            Toast.makeText(this, R.string.no_name, Toast.LENGTH_LONG).show();
            return;
        }

        // Create a ContentValues object where column names are the keys,
        // and book attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(BookContract.BookEntry.COLUMN_BOOK_NAME, nameString);

        // If the price or quantity is not provided by the user, don't try to part the string into an
        // integer value.
        //if the user did not enter anything, do not save the book
        //if he entered smth that could be parsed as number, use 0 by defaulr
        int price_num = 0;
        int quantity_num = 0;

        if (!TextUtils.isEmpty(price)) {
            try {
                price_num = Integer.parseInt(price);

                if (price_num < 0) {
                    price_num = 0;
                }
            } catch (java.lang.NumberFormatException e) {
                Log.v(LOG_TAG, getString(R.string.invalid_number));
            }
        } else {
            Toast.makeText(this, R.string.no_price, Toast.LENGTH_LONG).show();
            return;
        }

        values.put(BookContract.BookEntry.COLUMN_BOOK_PRICE, price_num);

        //if the user did not enter anything, do not save the book
        //if he entered smth that could be parsed as number, use 0 by default
        if (!TextUtils.isEmpty(quantity)) {
            try {
                quantity_num = Integer.parseInt(quantity);
                if (quantity_num < 0) {
                    quantity_num = 0;
                }
            } catch (java.lang.NumberFormatException e) {
                Log.v(LOG_TAG, getString(R.string.invalid_number));

            }
        } else {
            Toast.makeText(this, R.string.no_quantity, Toast.LENGTH_LONG).show();
            return;
        }
        values.put(BookContract.BookEntry.COLUMN_BOOK_QUANTITY, quantity_num);

        //check if the user has provided the image
        if (mImageUri != null) {
            values.put(BookContract.BookEntry.COLUMN_BOOK_PIC, mImageUri.toString());

        } else {
            Toast.makeText(this, R.string.no_pic, Toast.LENGTH_LONG).show();
            return;
        }

        // Determine if this is a new or existing book by checking if mCurrentBookUri is null or not
        if (mCurrentBookUri == null) {
            // This is a NEW book, so insert a new book into the provider,

            Uri newUri = getContentResolver().insert(BookContract.BookEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_book_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_book_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING book, so update the info
            int rowsAffected = getContentResolver().update(mCurrentBookUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_book_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_book_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save book to database
                saveBook();
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option

            case android.R.id.home:
                // If the book hasn't changed, continue with navigating up to parent activity
                // which is the {@link MainActivity}.
                if (!mBookHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the book hasn't changed, continue with handling back button press
        if (!mBookHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all book attributes, define a projection that contains
        // all columns from the book table
        String[] projection = {
                BookContract.BookEntry._ID,
                BookContract.BookEntry.COLUMN_BOOK_NAME,
                BookContract.BookEntry.COLUMN_BOOK_PRICE,
                BookContract.BookEntry.COLUMN_BOOK_QUANTITY,
                BookContract.BookEntry.COLUMN_BOOK_PIC
        };

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentBookUri,         // Query the content URI for the current book
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of book attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(BookContract.BookEntry.COLUMN_BOOK_NAME);
            int priceColumnIndex = cursor.getColumnIndex(BookContract.BookEntry.COLUMN_BOOK_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(BookContract.BookEntry.COLUMN_BOOK_QUANTITY);
            int picColumnIndex = cursor.getColumnIndex(BookContract.BookEntry.COLUMN_BOOK_PIC);


            // Extract out the value from the Cursor for the given column index
            name = cursor.getString(nameColumnIndex);
            int price = cursor.getInt(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);

            //we allow the picture to be null, so we should check the case did not add the picture
            try {
                String picString = cursor.getString(picColumnIndex);

                if (!picString.isEmpty()) {

                    mImageUri = Uri.parse(picString);
                    Log.v("pic", "" + mImageUri);
                    mImageBook.setImageURI(mImageUri);
                }
            } catch (java.lang.NullPointerException e) {
                Log.v(LOG_TAG, "No picture was found");
            }

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mPriceEditText.setText(Integer.toString(price));
            mQuantityEditText.setText(Integer.toString(quantity));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the book.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prompt the user to confirm that they want to delete this book.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the book.
                deleteBook();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the book.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the book in the database.
     */
    private void deleteBook() {
        // Only perform the delete if this is an existing book.
        if (mCurrentBookUri != null) {
            // Call the ContentResolver to delete the book at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentBookUri
            // content URI already identifies the book that we want.

            int rowsDeleted = getContentResolver().delete(mCurrentBookUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_book_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_book_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mImageUri != null) {
            outState.putString(STATE_IMAGE_URI, mImageUri.toString());
        }
    }

    @Override
    //reload the pic in case the phone rotates
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_IMAGE_URI)) {

            if (!savedInstanceState.getString(STATE_IMAGE_URI).isEmpty()) {

                mImageUri = Uri.parse(savedInstanceState.getString(STATE_IMAGE_URI));
                mImageBook.setImageURI(mImageUri);
            }
        }
    }


}
