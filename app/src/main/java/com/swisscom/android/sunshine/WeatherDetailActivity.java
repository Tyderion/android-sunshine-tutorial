package com.swisscom.android.sunshine;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.swisscom.android.sunshine.data.WeatherContract;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class WeatherDetailActivity extends ActionBarActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.weather_detail, menu);


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placehoplder fragment containing a simple view.
     */
    public static class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>  {

        public static final int DETAIL_LOADER = 0;
        public static final String DATE_KEY = "date";
        public static final String LOCATION_KEY = "location";


        @InjectView(R.id.weather_detail_date_textview) TextView dateTextView;
        @InjectView(R.id.weather_detail_forecast_textview) TextView forecastTextView;
        @InjectView(R.id.weather_detail_high_textview) TextView hightTempTextView;
        @InjectView(R.id.weather_detail_low_textview) TextView lowTempTextView;


        private final static String LOG_TAG = DetailFragment.class.getSimpleName();
        private static final String hashtag = " - #SunshineApp";
        private String date = "";
        private String mForecastString;
        private String mLocation;

        public DetailFragment() {

        }
        private static final String[] FORECAST_COLUMNS = {
                // In this case the id needs to be fully qualified with a table name, since
                // the content provider joins the location & weather tables in the background
                // (both have an _id column)
                // On the one hand, that's annoying.  On the other, you can search the weather table
                // using the location set by the user, which is only in the Location table.
                // So the convenience is worth it.
                WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
                WeatherContract.WeatherEntry.COLUMN_DATETEXT,
                WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTINGS
        };

        // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
        // must change.
        public static final int COL_WEATHER_ID = 0;
        public static final int COL_WEATHER_DATE = 1;
        public static final int COL_WEATHER_DESC = 2;
        public static final int COL_WEATHER_MAX_TEMP = 3;
        public static final int COL_WEATHER_MIN_TEMP = 4;
        public static final int COL_LOCATION_SETTING = 5;

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            if (null != savedInstanceState && savedInstanceState.containsKey(LOCATION_KEY)) {
                mLocation = savedInstanceState.getString(LOCATION_KEY);
            }
            getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString(LOCATION_KEY, mLocation);
        }

        @Override
        public void onResume() {
            super.onResume();
            if (null != mLocation && !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
                getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
            }
        }

        @Override
        public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

            Intent intent = getActivity().getIntent();
            if (intent.hasExtra(DATE_KEY)) {
                date = intent.getStringExtra(DATE_KEY);
            }
            // Sort order:  Ascending, by date.
            String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";

            mLocation = Utility.getPreferredLocation(getActivity());
            Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                    mLocation, date);

            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    getActivity(),
                    weatherForLocationUri,
                    FORECAST_COLUMNS,
                    null,
                    null,
                    sortOrder
            );
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
            if (null != cursor && cursor.moveToFirst()) {
                dateTextView.setText(Utility.formatDate(cursor.getString(COL_WEATHER_DATE)));
                forecastTextView.setText(cursor.getString(COL_WEATHER_DESC));
                final boolean isMetric = Utility.isMetric(getActivity());
                hightTempTextView.setText(Utility.formatTemperature(cursor.getDouble(COL_WEATHER_MAX_TEMP), isMetric));
                lowTempTextView.setText(Utility.formatTemperature(cursor.getDouble(COL_WEATHER_MIN_TEMP), isMetric));

                mForecastString = String.format("%s - %s - %s°/%s°",
                        dateTextView.getText(), forecastTextView.getText(), lowTempTextView.getText(),  hightTempTextView.getText());
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            inflater.inflate(R.menu.weather_detail_fragment, menu);
            MenuItem item = menu.findItem(R.id.action_share);

            // Fetch and store ShareActionProvider
            // Fetch and store ShareActionProvider
//            ShareActionProvider shareActionProvider = (ShareActionProvider) item.getActionProvider();
            android.support.v7.widget.ShareActionProvider shareActionProvider = (android.support.v7.widget.ShareActionProvider) MenuItemCompat.getActionProvider(item);
            shareActionProvider.setShareIntent(createShareIntent());
        }


        private Intent createShareIntent() {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.putExtra(Intent.EXTRA_TEXT, mForecastString);
            shareIntent.setType("text/plain");
            return shareIntent;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_weather_detail, container, false);
            ButterKnife.inject(this, rootView);
            return rootView;
        }
    }
}
