package com.example.gpsdrawing;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.IntentSender;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class MainActivity extends Activity implements 
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener,
LocationListener{

	/*
	 * Define a Request code to send to google play services
	 * This code is returned in Activity.onActivityResult
	 */
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000; 
	
	// Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    
    
    protected static MainActivity parent;
	
    protected LocationClient mLocationClient;
	protected LocationRequest mLocationRequest;
	
	protected boolean mLocationClientConnected;
	protected String lastLocation;
	
	protected List<Point> points;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        PlaceholderFragment fragment = new PlaceholderFragment(); 
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }
        
        mLocationClient = new LocationClient(this,this,this);
        
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        
        lastLocation = "";
        
        mLocationClientConnected = false;
        
        points = new ArrayList<Point>();
        
        parent = this;
        
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	mLocationClient.connect();
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	mLocationClient.disconnect();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

    	protected static EditText groupIdText, drawIdText;
    	
    	protected static TextView locationView, bufferView;
    	
    	protected static RadioGroup colorRadio;
    	public static int r,g,b;
    	
    	protected static Button uploadButton;

    	protected static ToggleButton penToggle;
    	
    	
    	
        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            
            groupIdText = (EditText) rootView.findViewById(R.id.etGroupId);
            drawIdText = (EditText) rootView.findViewById(R.id.etDrawId);
            
            locationView = (TextView) rootView.findViewById(R.id.tvLocationView);
            bufferView = (TextView) rootView.findViewById(R.id.tvBufferView);
            
            colorRadio = (RadioGroup) rootView.findViewById(R.id.rgColor);
            
            colorRadio.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					r = g = b = 0;
					switch(checkedId){
						case R.id.rbRed:
							r = 255;
							break;
						case R.id.rbGreen:
							g = 255;
							break;
						case R.id.rbBlue:
							b = 255;
							break;
						default:
							r = 255;
							break;
					}
					
				}
			});

            uploadButton = (Button) rootView.findViewById(R.id.btUpload);
            
            penToggle = (ToggleButton) rootView.findViewById(R.id.tbPen);
            penToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
    			
    			@Override
    			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    				if (isChecked){
    					if((parent.mLocationClient != null)&&(parent.mLocationClientConnected)&&(parent.mLocationRequest != null))
    						parent.mLocationClient.requestLocationUpdates(parent.mLocationRequest, parent);
    				}else{
    					if (parent.mLocationClient.isConnected()) {
    			            /*
    			             * Remove location updates for a listener.
    			             * The current Activity is the listener, so
    			             * the argument is "this".
    			             */
    						parent.mLocationClient.removeLocationUpdates(parent);
    			        }
    				}
    			}
    		});
            
            return rootView;
        }
    }

    
    private void updateUI() {
    	runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				if((PlaceholderFragment.locationView != null)&&(lastLocation != null)&&
						(PlaceholderFragment.bufferView != null)) {
					PlaceholderFragment.locationView.setText(lastLocation);
					PlaceholderFragment.bufferView.setText(""+points.size());
				}
			}
		});
    }

    
    /* ********* GooglePlay methods implementation ********* */
    
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		/*
		 * Google Play services can resolve some errors it detects.
		 * If the error has a resolution, try sending an Intent to
		 * start a Google Play services activity that can resolve
		 * error.
		 * */
		
		if(result.hasResolution()) {
			try {
				//Starts an Activity that tries to resolve the error
				result.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
				
				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 * */
			} catch (IntentSender.SendIntentException e) {
				//Log the error
				e.printStackTrace();
			}
		
		}else {
			/*
			 * If no resolution is available, display a dialog to the
			 * user with the error. 
			 * */
			Dialog errorDialog = GooglePlayServicesUtil
					.getErrorDialog(result.getErrorCode(), this, 
							CONNECTION_FAILURE_RESOLUTION_REQUEST);
			
			// If Google Play services can provide an error dialog
			if(errorDialog!=null) {
				// Create a new DialogFragment for the error dialog
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				// Set the Dialog in the DialogFragment
				errorFragment.setDialog(errorDialog);
				// Show the error dialog in the DialogFragment
				errorFragment.show(getFragmentManager(), "Location Updates");
			}
		}
	}


	/*
	 * Called by Location Services when the request to connect the
	 * client finishes successfully. At this point, you can
	 * request the current location or start periodic updates
	 * */
	@Override
	public void onConnected(Bundle connectionHint) {
		// Display the connection status
		Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
		
		mLocationClientConnected = true;
		
	}


	@Override
	public void onDisconnected() {
		//TODO 
	}
	
	
	
	// LocationListener
	@Override
	public void onLocationChanged(Location location) {
		this.lastLocation = "("+location.getLatitude()+","+location.getLongitude()+")";
		updateUI();
		
		points.add(new Point((int)location.getLatitude(),(int)location.getLongitude()));
		
		//upload()
		System.out.println("GroupID: "+PlaceholderFragment.groupIdText.getText()+"\n"+
							"DrawID: "+PlaceholderFragment.drawIdText.getText()+"\n"+
							"Stroke: StrokeName? \n"+
							"R:"+PlaceholderFragment.r+" G:"+PlaceholderFragment.g+" B:"+PlaceholderFragment.b+"\n"+
							points);
	}
	
	
	/* ********* Helper Class ********* */
	//Defines a DialogFragment that displays the error dialog
	public static class ErrorDialogFragment extends DialogFragment {
		//Global field to contain the error dialog
		private Dialog mDialog;
		
		//Default constructor. Sets the dialog field to null
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}
		
		//Set the dialog to display
		public void setDialog(Dialog dialog){
			mDialog = dialog;
		}
		
		//Return a Dialog to the DialogFragment
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}

}
