package elegantuniv.atthack;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.RemoteInput;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.os.Handler;

import sensordrone.DroneEventListener;
import sensordrone.DroneEventObject;
import sensordrone.DroneStatusListener;
import sensordrone.android.tools.DroneConnectionHelper;
import sensordrone.android.tools.DroneQSStreamer;
import sensordrone.android.tools.DroneStreamer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

// Don't let eclipse import android.widget.TableLayout.LayoutParams for your TableRows!

/**
 * Sensordrone Control for the Sensordrone
 * 
 * Built against Android-10
 * 
 * Build using SDAndroidLib 1.3.1
 * 
 * @author Sensorcon, Inc
 * 
 */
public class SensordroneControl extends Activity {

	/*
	 * Preferences
	 */
	private SharedPreferences sdcPreferences;

	/*
	 * We put our Drone object in a class that extends Application so it can be
	 * accessed in multiple activities.
	 */
	private DroneApplication droneApp;

	// A ConnectionBLinker from the SDHelper Library
	private DroneStreamer myBlinker;

	// Our Listeners
	private DroneEventListener deListener;
	private DroneStatusListener dsListener;

	// An int[] that will hold the QS_TYPEs for our sensors of interest
	private int[] qsSensors;

	// Text to display
	private static final String[] SENSOR_NAMES = { "Temperature (Ambient)",
			"Humidity", "Pressure", "Object Temperature (IR)",
			"Illuminance (calculated)", "Precision Gas (CO equivalent)",
			"Proximity Capacitance", "External Voltage (0-3V)",
			"Altitude (calculated)" };

	// Figure out how many sensors we have based on the length of our labels
	private int numberOfSensors = SENSOR_NAMES.length;

	// GUI Stuff
	private TableLayout onOffLayout;
	private ToggleButton[] toggleButtons = new ToggleButton[numberOfSensors];
	private TextView tvConnectionStatus;
	private TextView tvConnectInfo;
	private TextView[] tvSensorValues = new TextView[numberOfSensors];
	private TextView[] tvLabel = new TextView[numberOfSensors];
	private LinearLayout logoLayout;
	private TextView logoText;
	private ImageView logoImage;
	private TableRow[] sensorRow = new TableRow[numberOfSensors];

	// This is added for the battery voltage
	private TableRow bvRow;
	private ToggleButton bvToggle;
	private TextView bvLabel;
	private TextView bvValue;

    private Notification mNotification;

	// Another object from the SDHelper library. It helps us set up our
	// pseudo streaming
	private DroneQSStreamer[] streamerArray = new DroneQSStreamer[numberOfSensors];

	// We only want to notify of a low battery once,
	// but the event might be triggered multiple times.
	// We use this to try and show it only once
	private boolean lowbatNotify;

    // Toggle our LED
    private boolean ledToggle = true;

	/*
	 * Our TableRow layout
	 */
	private LayoutParams trLayout = new LayoutParams(LayoutParams.MATCH_PARENT,
			LayoutParams.MATCH_PARENT);
	/*
	 * Our TextView label layout
	 */
	private LayoutParams tvLayout = new LayoutParams(LayoutParams.MATCH_PARENT,
			LayoutParams.MATCH_PARENT, 0.45f);

	/*
	 * Our ToggleButton layout
	 */
	private LayoutParams tbLayout = new LayoutParams(LayoutParams.MATCH_PARENT,
			LayoutParams.MATCH_PARENT, 0.1f);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Get out Application so we have access to our Drone
		droneApp = (DroneApplication) getApplication();

		// Initialize SharedPreferences
		sdcPreferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		qsSensors = new int[] { droneApp.myDrone.QS_TYPE_TEMPERATURE,
				droneApp.myDrone.QS_TYPE_HUMIDITY,
				droneApp.myDrone.QS_TYPE_PRESSURE,
				droneApp.myDrone.QS_TYPE_IR_TEMPERATURE,
				droneApp.myDrone.QS_TYPE_RGBC,
				droneApp.myDrone.QS_TYPE_PRECISION_GAS,
				droneApp.myDrone.QS_TYPE_CAPACITANCE,
				droneApp.myDrone.QS_TYPE_ADC, droneApp.myDrone.QS_TYPE_ALTITUDE };

		// This will Blink our Drone, once a second, Blue
        myBlinker = new DroneStreamer(droneApp.myDrone, 1000) {
            @Override
            public void repeatableTask() {
                if (ledToggle) {
                droneApp.myDrone.setLEDs(0, 0, 126);
                } else {
                    droneApp.myDrone.setLEDs(0,0,0);
                }
                ledToggle = !ledToggle;
            }
        };

		// Set up the TableRows
		for (int i = 0; i < numberOfSensors; i++) {

			// The clickListener will need a final type of i
			final int counter = i;

			sensorRow[i] = new TableRow(this);
			sensorRow[i].setPadding(10, 10, 10, 0);

			toggleButtons[i] = new ToggleButton(this);
			tvSensorValues[i] = new TextView(this);
			tvLabel[i] = new TextView(this);
			streamerArray[i] = new DroneQSStreamer(droneApp.myDrone, qsSensors[i]);

			sensorRow[i].setLayoutParams(trLayout); // Set the layout
			tvLabel[i].setText(SENSOR_NAMES[i]); // Set the text
			tvLabel[i].setLayoutParams(tvLayout); // Set the layout

			tvSensorValues[i].setBackgroundResource(R.drawable.valuegradient);
			tvSensorValues[i].setTextColor(Color.WHITE);
			tvSensorValues[i].setGravity(Gravity.CENTER);
			tvSensorValues[i].setText("--"); // Start off with -- for the
												// sensor value on create
			tvSensorValues[i].setLayoutParams(tvLayout); // Set the layout

			// This ClickListener will handle Graphing
			tvSensorValues[i].setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// Only register a click if the sensor is enabled
					if (droneApp.myDrone.quickStatus(qsSensors[counter])) {
//						Intent myIntent = new Intent(getApplicationContext(),
//								GraphActivity.class);
//						myIntent.putExtra("SensorName", SENSOR_NAMES[counter]);
//						myIntent.putExtra("quickInt", qsSensors[counter]);
//						startActivity(myIntent);
					} else {
						//
					}

				}
			});

			toggleButtons[i].setLayoutParams(tbLayout); // Set the Layout of
														// our ToggleButton
			// toggleButtons[i].setChecked(droneApp.myDrone.quickStatus(qsSensors[i]));
			// // Set it clicked if the sensor is already enabled

			/*
			 * Add all of our UI elements to the TableRow. (Order is important!)
			 */
			sensorRow[i].addView(toggleButtons[i]);
			sensorRow[i].addView(tvLabel[i]);
			sensorRow[i].addView(tvSensorValues[i]);

			/*
			 * Set up our ToggleButtons to turn on/off our sensors and start
			 * psuedo-streaming
			 */
			toggleButtons[i]
					.setOnCheckedChangeListener(new OnCheckedChangeListener() {

						/*
						 * The general behavior of the program is as follows:
						 * 
						 * When a sensor is enabled: 1) When this button is
						 * toggled, it executes the Drone qsEnable method for
						 * the sensor. This updates the sensors status, and
						 * triggers its section of the DroneStatusListener. It
						 * also sets up the myStreamer object used to make
						 * measurements at a specified interval. 2) When the
						 * DroneStatusListener is triggered, it runs the
						 * sensor's measurement method. When the measurement
						 * comes back, it triggers the DroneEventListener
						 * section for that sensor. There, it updates the
						 * display with it's value, and uses the myStreamer
						 * handler to ask for a measurement again automatically
						 * at the defined interval. 2-b) This repeats until the
						 * myStreamer object is disabled.
						 * 
						 * When a sensor is disabled: 1) The mySteamer object is
						 * stopped, preventing more measurements from being
						 * requested. The Drone qsDisable method is called for
						 * the appropriate sensor (This also triggers the
						 * corresponding DroneStatusEvent!).
						 */

						/*
						 * Turn the sensors on/off
						 */
						@Override
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							// If the Sensordrone is not connected, don't
							// allow toggling of the sensors
							if (!droneApp.myDrone.isConnected) {
								toggleButtons[counter].setChecked(false);
							} else {
								if (toggleButtons[counter].isChecked()) {

									// Enable our steamer
									streamerArray[counter].enable();
									// Enable the sensor
									droneApp.myDrone
											.quickEnable(qsSensors[counter]);

								} else {
									// Stop taking measurements
									streamerArray[counter].disable();

									// Disable the sensor
									droneApp.myDrone
											.quickDisable(qsSensors[counter]);

								}
							}
						}
					});
		}

		// Connection Status TextView
		tvConnectionStatus = new TextView(this);
		tvConnectionStatus.setText("Disconnected");
		tvConnectionStatus.setTextColor(Color.WHITE);
		tvConnectionStatus.setTextSize(18);
		tvConnectionStatus.setPadding(10, 10, 10, 10);
		tvConnectionStatus.setGravity(Gravity.CENTER);
		// Tell people how to connect
		tvConnectInfo = new TextView(this);
		tvConnectInfo.setText("Connect from your device's menu");
		tvConnectInfo.setTextColor(Color.WHITE);
		tvConnectInfo.setTextSize(18);
		tvConnectInfo.setPadding(10, 10, 10, 10);
		tvConnectInfo.setGravity(Gravity.CENTER);
		tvConnectInfo.setVisibility(TextView.VISIBLE);

		// Our top Picture Thing
		logoLayout = new LinearLayout(this);
		logoText = new TextView(this);
		logoText.setBackgroundColor(Color.BLACK);
		logoText.setTextColor(Color.WHITE);
		logoText.setText("Sensordrone\t\t\nControl");
		logoText.setTextSize(22);
		logoImage = new ImageView(this);
		Drawable img = getResources().getDrawable(R.drawable.apollo1725);
		logoImage.setImageDrawable(img);
		logoLayout.addView(logoText);
		logoLayout.addView(logoImage);
		logoLayout.setGravity(Gravity.CENTER);
		logoLayout.setBackgroundResource(R.drawable.logogradient);
		logoLayout.setPadding(10, 10, 10, 10);

		// Measuring battery voltage is not part of the API's quickSystem,
		// so we will have
		// to set up a table row manually here
		bvRow = new TableRow(this);
		bvRow.setLayoutParams(trLayout);
		bvRow.setPadding(10, 10, 10, 0);
		bvToggle = new ToggleButton(this);
		bvToggle.setLayoutParams(tbLayout);
		bvLabel = new TextView(this);
		bvLabel.setLayoutParams(tvLayout);
		bvLabel.setText("Battery Voltage");
		bvValue = new TextView(this);
		bvValue.setBackgroundResource(R.drawable.valuegradient);
		bvValue.setTextColor(Color.WHITE);
		bvValue.setGravity(Gravity.CENTER);
		bvValue.setLayoutParams(tvLayout);
		bvValue.setText("--");

		// Measure the battery at the default rate (once a second)
		final DroneStreamer bvStreamer = new DroneStreamer(droneApp.myDrone, droneApp.defaultRate) {
            @Override
            public void repeatableTask() {
                droneApp.myDrone.measureBatteryVoltage();
            }
        };

		// Set up our graphing
		bvValue.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Only graph if the toggle button is checked
				if (bvToggle.isChecked()) {
//					Intent myIntent = new Intent(getApplicationContext(),
//							GraphActivity.class);
//					myIntent.putExtra("SensorName", "Battery Voltage");
//					// We'll use a made-up number outside of the range of
//					// the quickSystem
//					// that we can parse for the battery voltage
//					myIntent.putExtra("quickInt", 42);
//					startActivity(myIntent);
				}
			}
		});

		// Set up our toggle button
		bvToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// Don't do anything if not connected
				if (!droneApp.myDrone.isConnected) {
					bvToggle.setChecked(false);
				} else {
					if (bvToggle.isChecked()) {
						// Enable our steamer
						bvStreamer.start();
						// Measure the voltage once to trigger streaming
						droneApp.myDrone.measureBatteryVoltage();

					} else {
						// Stop taking measurements
						bvStreamer.stop();

					}
				}
			}
		});

		// Add it all to the row. We'll add the row to the main layout in
		// onCreate
		bvRow.addView(bvToggle);
		bvRow.addView(bvLabel);
		bvRow.addView(bvValue);

		/*
		 * Let's set up our Drone Event Listener.
		 * 
		 * See adcMeasured for the general flow for when a sensor is measured.
		 */
		deListener = new DroneEventListener() {

			@Override
			public void adcMeasured(DroneEventObject arg0) {
				// This is triggered the the external ADC pin is measured

				// Update our display with the measured value
				tvUpdate(7,
						tvSensorValues[7],
						String.format("%.3f",
								droneApp.myDrone.externalADC_Volts) + " V");
				// Ask for another measurement
				// (droneApp.streamingRate has been set to 1 second, so
				// every time the ADC is measured
				// it will measure again in one second)
				streamerArray[7].streamHandler.postDelayed(streamerArray[7],
						droneApp.streamingRate);
			}

			@Override
			public void altitudeMeasured(DroneEventObject arg0) {
				int pref = sdcPreferences.getInt(SDPreferences.ALTITUDE_UNIT,
						SDPreferences.FEET);
				if (pref == SDPreferences.FEET) {
					tvUpdate(8,
							tvSensorValues[8],
							String.format("%.0f",
									droneApp.myDrone.altitude_Feet) + " Ft");
				} else if (pref == SDPreferences.MILES) {
					tvUpdate(8,
							tvSensorValues[8],
							String.format(
									"%.02f",
									droneApp.myDrone.altitude_Feet * 0.000189394)
									+ " Mi");
				} else if (pref == SDPreferences.METER) {
					tvUpdate(8,
							tvSensorValues[8],
							String.format("%.0f",
									droneApp.myDrone.altitude_Meters) + " m");
				} else if (pref == SDPreferences.KILOMETER) {
					tvUpdate(8,
							tvSensorValues[8],
							String.format("%.03f",
									droneApp.myDrone.altitude_Meters / 1000)
									+ " km");
				}

				streamerArray[8].streamHandler.postDelayed(streamerArray[8],
						droneApp.streamingRate);

			}

			@Override
			public void capacitanceMeasured(DroneEventObject arg0) {
				tvUpdate(6,
						tvSensorValues[6],
						String.format("%.0f",
								droneApp.myDrone.capacitance_femtoFarad)
								+ " fF");
				streamerArray[6].streamHandler.postDelayed(streamerArray[6],
						droneApp.streamingRate);

			}

			@Override
			public void connectEvent(DroneEventObject arg0) {

				// Since we are adding SharedPreferences to store unit
				// preferences,
				// we might as well store the last MAC there. Now we can
				// press re-connect
				// to always try and connect to the last Drone (not just the
				// last one per
				// app instance)

				Editor prefEditor = sdcPreferences.edit();
				prefEditor.putString(SDPreferences.LAST_MAC,
						droneApp.myDrone.lastMAC);
				prefEditor.commit();

				// Things to do when we connect to a Sensordrone
				quickMessage("Connected!");
				tvUpdate(-1,tvConnectionStatus, "Connected to: "
						+ droneApp.myDrone.lastMAC);

				// Turn on our blinker
				myBlinker.start();
				// People don't need to know how to connect if they are
				// already connected
				tvConnectInfo.setVisibility(TextView.INVISIBLE);
				// Notify if there is a low battery
				lowbatNotify = true;
			}

			@Override
			public void connectionLostEvent(DroneEventObject arg0) {

				// Things to do if we think the connection has been lost.

				// Turn off the blinker
				myBlinker.stop();

				// notify the user
				tvUpdate(-2,tvConnectionStatus, "Connection Lost!");
				quickMessage("Connection lost! Trying to re-connect!");

				// Try to reconnect once, automatically
				if (droneApp.myDrone.btConnect(droneApp.myDrone.lastMAC)) {
					// A brief pause
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					connectionLostReconnect();
				} else {
					quickMessage("Re-connect failed");
					doOnDisconnect();
				}
			}

			@Override
			public void customEvent(DroneEventObject arg0) {

			}

			@Override
			public void disconnectEvent(DroneEventObject arg0) {
				// notify the user
				quickMessage("Disconnected!");
				tvConnectionStatus.setText("Disconnected");
			}

			@Override
			public void oxidizingGasMeasured(DroneEventObject arg0) {

			}

			@Override
			public void reducingGasMeasured(DroneEventObject arg0) {

			}

			@Override
			public void humidityMeasured(DroneEventObject arg0) {
				tvUpdate(1,
						tvSensorValues[1],
						String.format("%.1f", droneApp.myDrone.humidity_Percent)
								+ " %");
				streamerArray[1].streamHandler.postDelayed(streamerArray[1],
						droneApp.streamingRate);

			}

			@Override
			public void i2cRead(DroneEventObject arg0) {

			}

			@Override
			public void irTemperatureMeasured(DroneEventObject arg0) {
				int pref = sdcPreferences.getInt(
						SDPreferences.IR_TEMPERATURE_UNIT,
						SDPreferences.FAHRENHEIT);
				if (pref == SDPreferences.FAHRENHEIT) {
					tvUpdate(3,
							tvSensorValues[3],
							String.format("%.1f",
									droneApp.myDrone.irTemperature_Fahrenheit)
									+ " \u00B0F");
				} else if (pref == SDPreferences.CELSIUS) {
					tvUpdate(3,
							tvSensorValues[3],
							String.format("%.1f",
									droneApp.myDrone.irTemperature_Celsius)
									+ " \u00B0C");
				} else if (pref == SDPreferences.KELVIN) {
					tvUpdate(3,
							tvSensorValues[3],
							String.format("%.1f",
									droneApp.myDrone.irTemperature_Kelvin)
									+ " K");
				}
				streamerArray[3].streamHandler.postDelayed(streamerArray[3],
						droneApp.streamingRate);

			}

			@Override
			public void precisionGasMeasured(DroneEventObject arg0) {
				tvUpdate(5,
						tvSensorValues[5],
						String.format("%.1f",
								droneApp.myDrone.precisionGas_ppmCarbonMonoxide)
								+ " ppm");
				streamerArray[5].streamHandler.postDelayed(streamerArray[5],
						droneApp.streamingRate);

			}

			@Override
			public void pressureMeasured(DroneEventObject arg0) {
				int pref = sdcPreferences.getInt(SDPreferences.PRESSURE_UNIT,
						SDPreferences.PASCAL);
				if (pref == SDPreferences.PASCAL) {
					tvUpdate(2,
							tvSensorValues[2],
							String.format("%.0f",
									droneApp.myDrone.pressure_Pascals) + " Pa");
				} else if (pref == SDPreferences.KILOPASCAL) {
					tvUpdate(2,
							tvSensorValues[2],
							String.format("%.2f",
									droneApp.myDrone.pressure_Pascals / 1000)
									+ " kPa");
				}  else if (pref == SDPreferences.HECTOPASCAL) {
                    tvUpdate(2,
                            tvSensorValues[2],
                            String.format("%.2f",
                                    droneApp.myDrone.pressure_Pascals / 100)
                                    + " hPa");

                } else if (pref == SDPreferences.ATMOSPHERE) {
					tvUpdate(2,
							tvSensorValues[2],
							String.format("%.2f",
									droneApp.myDrone.pressure_Atmospheres)
									+ " Atm");
				} else if (pref == SDPreferences.MMHG) {
					tvUpdate(2,
							tvSensorValues[2],
							String.format("%.0f",
									droneApp.myDrone.pressure_Torr) + " mmHg");
				} else if (pref == SDPreferences.INHG) {
					tvUpdate(2,
							tvSensorValues[2],
							String.format(
									"%.2f",
									droneApp.myDrone.pressure_Torr * 0.0393700732914)
									+ " inHg");
				}
				streamerArray[2].streamHandler.postDelayed(streamerArray[2],
						droneApp.streamingRate);

			}

			@Override
			public void rgbcMeasured(DroneEventObject arg0) {
				// The Lux value is calibrated for a (mostly) broadband
				// light source.
				// Pointing it at a narrow band light source (like and LED)
				// will bias the color channels, and provide a "wonky"
				// number.
				// Just for a nice look, we won't show a negative number.
				String msg = "";
				if (droneApp.myDrone.rgbcLux >= 0) {
					msg = String.format("%.0f", droneApp.myDrone.rgbcLux)
							+ " Lux";
				} else {
					msg = String.format("%.0f", 0.0) + " Lux";

                    postWearNotification1("Outside home");
				}
				tvUpdate(4, tvSensorValues[4], msg);
				streamerArray[4].streamHandler.postDelayed(streamerArray[4],
						droneApp.streamingRate);

                Log.d("atthack", msg);
			}

			@Override
			public void temperatureMeasured(DroneEventObject arg0) {
				int pref = sdcPreferences
						.getInt(SDPreferences.TEMPERATURE_UNIT,
								SDPreferences.FAHRENHEIT);
				if (pref == SDPreferences.FAHRENHEIT) {
					tvUpdate(0,
							tvSensorValues[0],
							String.format("%.1f",
									droneApp.myDrone.temperature_Fahrenheit)
									+ "  \u00B0F");
				} else if (pref == SDPreferences.CELSIUS) {
					tvUpdate(0,
							tvSensorValues[0],
							String.format("%.1f",
									droneApp.myDrone.temperature_Celsius)
									+ "  \u00B0C");
				} else if (pref == SDPreferences.KELVIN) {
					tvUpdate(0,
							tvSensorValues[0],
							String.format(
									"%.1f",
									droneApp.myDrone.temperature_Kelvin)
									+ "  K");
				}
				streamerArray[0].streamHandler.postDelayed(streamerArray[0],
						droneApp.streamingRate);

			}

			@Override
			public void uartRead(DroneEventObject arg0) {

			}

			@Override
			public void unknown(DroneEventObject arg0) {

			}

			@Override
			public void usbUartRead(DroneEventObject arg0) {

			}
		};

		/*
		 * Set up our status listener
		 * 
		 * see adcStatus for the general flow for sensors.
		 */
		dsListener = new DroneStatusListener() {

			@Override
			public void adcStatus(DroneEventObject arg0) {
				// This is triggered when the status of the external ADC has
				// been
				// enable, disabled, or checked.

				// If status has been triggered to true (on)
				if (droneApp.myDrone.adcStatus) {
					// then start the streaming by taking the first
					// measurement
					streamerArray[7].run();
				}
				// Don't do anything if false (off)
			}

			@Override
			public void altitudeStatus(DroneEventObject arg0) {
				if (droneApp.myDrone.altitudeStatus) {
					streamerArray[8].run();
				}

			}

			@Override
			public void batteryVoltageStatus(DroneEventObject arg0) {
				// This is triggered when the battery voltage has been
				// measured.
				String bVoltage = String.format("%.2f",
						droneApp.myDrone.batteryVoltage_Volts) + " V";
				tvUpdate(9, bvValue, bVoltage);
                // We might need to update the rate due to graphing
                bvStreamer.setRate(droneApp.streamingRate);
			}

			@Override
			public void capacitanceStatus(DroneEventObject arg0) {
				if (droneApp.myDrone.capacitanceStatus) {
					streamerArray[6].run();
				}
			}

			@Override
			public void chargingStatus(DroneEventObject arg0) {

			}

			@Override
			public void customStatus(DroneEventObject arg0) {

			}

			@Override
			public void humidityStatus(DroneEventObject arg0) {
				if (droneApp.myDrone.humidityStatus) {
					streamerArray[1].run();
				}

			}

			@Override
			public void irStatus(DroneEventObject arg0) {
				if (droneApp.myDrone.irTemperatureStatus) {
					streamerArray[3].run();
				}

			}

			@Override
			public void lowBatteryStatus(DroneEventObject arg0) {
				// If we get a low battery, notify the user
				// and disconnect

				// This might trigger a lot (making a call the the LEDS will
				// trigger it,
				// so the myBlinker will trigger this once a second.
				// calling myBlinker.disable() even sets LEDS off, which
				// will trigger it...

                // We wil also add in a voltage check, to allow users to use their
                // Sensordrone a little more
				if (lowbatNotify && droneApp.myDrone.batteryVoltage_Volts < 3.1) {
					lowbatNotify = false; // Set true again in connectEvent
					myBlinker.stop();
					doOnDisconnect(); // run our disconnect routine
					// Notify the user
					tvUpdate(-4, tvConnectionStatus, "Low Battery: Disconnected!");
					AlertInfo.lowBattery(SensordroneControl.this);
				}

			}

			@Override
			public void oxidizingGasStatus(DroneEventObject arg0) {

			}

			@Override
			public void precisionGasStatus(DroneEventObject arg0) {
				if (droneApp.myDrone.precisionGasStatus) {
					streamerArray[5].run();
				}

			}

			@Override
			public void pressureStatus(DroneEventObject arg0) {
				if (droneApp.myDrone.pressureStatus) {
					streamerArray[2].run();
				}

			}

			@Override
			public void reducingGasStatus(DroneEventObject arg0) {

			}

			@Override
			public void rgbcStatus(DroneEventObject arg0) {
				if (droneApp.myDrone.rgbcStatus) {
					streamerArray[4].run();
				}

			}

			@Override
			public void temperatureStatus(DroneEventObject arg0) {
				if (droneApp.myDrone.temperatureStatus) {
					streamerArray[0].run();
				}

			}

			@Override
			public void unknownStatus(DroneEventObject arg0) {

			}
		};

		onOffLayout = (TableLayout) findViewById(R.id.tlOnOff);

		onOffLayout.addView(logoLayout);

		for (int i = 0; i < numberOfSensors; i++) {
			onOffLayout.addView(sensorRow[i]);
		}
		onOffLayout.addView(bvRow);
		onOffLayout.addView(tvConnectionStatus);
		onOffLayout.addView(tvConnectInfo);

		droneApp.myDrone.registerDroneListener(deListener);
		droneApp.myDrone.registerDroneListener(dsListener);

        // Display a "What's New" Dialog if we've updated.
        // Get the current version code
        PackageInfo pinfo = null;
        try {
            pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            pinfo = null;
        }
        if (pinfo != null) {
            // Current Version
            final int currentVersionNumber = pinfo.versionCode;
            // Last Known Version
            int lastVersionNumber = sdcPreferences.getInt(SDPreferences.LAST_KNOWN_VERSION_CODE, -1);
            if (currentVersionNumber != lastVersionNumber) {
                // What's new?
                String msg = "IMPORTANT!\n\n";
                msg += "We have changed how the app connects to your Sensordrone! To connect to a Sensordrone, you will now need to pair the Sensordrone with your Android device via " +
                       "the System Settings!\n\n";
                msg += "Other changes include:\n\n";
                msg += " A 'What's New' dialog for updates.\n\n";
                msg += "Fixed a bug related to reading the battery voltage.\n\n";
                msg += "Lowered the threshold on battery voltage for automatically disconnecting the Sensordrone from the app\n\n";
                msg += "Fixed some typos.\n\n";
                msg += "Added in hPa units for pressure";

                Dialog dialog;
                AlertDialog.Builder builder = new AlertDialog.Builder(SensordroneControl.this);
                builder.setTitle("What's New");
                builder.setMessage(msg);
                builder.setPositiveButton("Display next time", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //
                    }
                });
                builder.setNegativeButton("Don't remind me again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Editor editor = sdcPreferences.edit();
                        editor.putInt(SDPreferences.LAST_KNOWN_VERSION_CODE, currentVersionNumber);
                        editor.commit();
                    }
                });
                dialog = builder.create();
                dialog.show();
            }

        }

//        receivingVoiceInput();
	}

    private void receivingVoiceInput(){
        int notificationId = 5;

        // Key for the string that's delivered in the action's intent
        final String EXTRA_VOICE_REPLY = "extra_voice_reply";

        String replyLabel = "make a call";

        RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_VOICE_REPLY)
                .setLabel(replyLabel)
                .build();

// Create an intent for the reply action
        Intent replyIntent = new Intent(this, ReplyActivity.class);
        PendingIntent replyPendingIntent =
                PendingIntent.getActivity(this, 0, replyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

// Create the reply action and add the remote input
        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(android.R.drawable.ic_menu_send, "Reply", replyPendingIntent)
                        .addRemoteInput(remoteInput)
                        .build();

// Build the notification and add the action via WearableExtender
        Notification notification =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_plusone_medium_off_client)
                        .setContentTitle("Title")
                        .setContentText("content")
                        .extend(new NotificationCompat.WearableExtender().addAction(action))
                        .build();

        // Issue the notification
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(this);
        notificationManager.notify(notificationId, notification);
    }

    private CharSequence getMessageText(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return remoteInput.getCharSequence("extra_voice_reply");
        }
        return null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if(intent.hasExtra("isIgnore")) {
            boolean isIgnore = intent.getBooleanExtra("isIgnore", true);
            if (isIgnore) {
                new ProcessNetTask().execute(null, null, null);
            }
        }
    }

	/*
	 * We will use some stuff from our Sensordrone Helper library
	 */
	public DroneConnectionHelper myHelper = new DroneConnectionHelper();

	@Override
	public void onDestroy() {
        NotificationManagerCompat.from(this).cancel(NotificationUtil.WEAR_NOTIFICAITON_ID);
		super.onDestroy();

		if (isFinishing()) {
			// Try and nicely shut down
			doOnDisconnect();

			droneApp.myDrone.unregisterDroneListener(deListener);
			droneApp.myDrone.unregisterDroneListener(dsListener);
		}
	}

	/*
	 * A function to display Toast Messages.
	 * 
	 * By having it run on the UI thread, we will be sure that the message is
	 * displays no matter what thread tries to use it.
	 */
	public void quickMessage(final String msg) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT)
						.show();
			}
		});

	}

	/*
	 * A function to update a TextView
	 * 
	 * We have it run on the UI thread to make sure it safely updates.
	 */
	public void tvUpdate(final int i, final TextView tv, final String msg) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				tv.setText(msg);
//				if ( i >= 0 )
//					sendPostRequest(String.valueOf(i), msg);
			}
		});
	}

	/**
	 * Things to do when we disconnect
	 */
	public void doOnDisconnect() {
		// Shut off any sensors that are on
		SensordroneControl.this.runOnUiThread(new Runnable() {

			@Override
			public void run() {

				// Turn off myBlinker
				myBlinker.stop();

				// Make sure the LEDs go off
				if (droneApp.myDrone.isConnected) {
					droneApp.myDrone.setLEDs(0, 0, 0);
				}

				// Toggle all of our buttons from On to Off
				for (int i = 0; i < numberOfSensors; i++) {
					// If it is on
					if (toggleButtons[i].isChecked()) {
						// Turn it off
						toggleButtons[i].performClick();
					}
				}

				// Don't forget the battery voltage button
				if (bvToggle.isChecked()) {
					bvToggle.performClick();
				}

				// Only try and disconnect if already connected
				if (droneApp.myDrone.isConnected) {
					droneApp.myDrone.disconnect();
				}

				// Remind people how to connect
				tvConnectInfo.setVisibility(TextView.VISIBLE);
			}
		});

	}

	// Stuff to do when we're trying to reconnect on connection lost
	public void connectionLostReconnect() {
		// Re-Toggle and sensors that were on
		SensordroneControl.this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				for (int i = 0; i < numberOfSensors; i++) {
					// If it is on
					if (toggleButtons[i].isChecked()) {
						// Turn it off and back on
						// This will trigger a measurement which will
						// get the psuedo streaming going again
						toggleButtons[i].performClick();
						toggleButtons[i].performClick();
					}
				}
				// Don't forget the battery voltage button
				if (bvToggle.isChecked()) {
					bvToggle.performClick();
					bvToggle.performClick();
				}
			}
		});
	}

	/*
	 * We will handle connect and disconnect in the menu.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menuConnect:
			if (!droneApp.myDrone.isConnected) {
				// This option is used to re-connect to the last connected MAC
				// from our SharedPreferences
				String prefLastMAC = sdcPreferences.getString(
						SDPreferences.LAST_MAC, "");
				if (!prefLastMAC.equals("")) {
					if (!droneApp.myDrone.btConnect(prefLastMAC)) {
						AlertInfo.connectFail(this);
					}
				} else {
					// Notify the user if no previous MAC was found.
					quickMessage("Last MAC not found... Please connect once");
				}
			} else {
				quickMessage("Already connected...");
			}
			break;

		case R.id.menuDisconnect:
			// Only disconnect if it's connected
			if (droneApp.myDrone.isConnected) {
				// Run our routine of things to do on disconnect
				doOnDisconnect();
			} else {
				quickMessage("Not currently connected..");
			}
			break;
		case R.id.menuScan:
			if (!droneApp.myDrone.isConnected) {
                myHelper.connectFromPairedDevices(droneApp.myDrone, SensordroneControl.this);
                // We now just use paired devices instead of scanning every time
//				myHelper.scanToConnect(droneApp.myDrone,
//                        SensordroneControl.this, this, false);
			} else {
				quickMessage("Please disconnect first");
			}
			break;

		case R.id.unitPrefs:
			Intent unitIntent = new Intent(getApplicationContext(),
					PrefsActivity.class);
			startActivity(unitIntent);
			break;

		// Help Menu items
		case R.id.infoConnections:
			AlertInfo.connectionInfo(this);
			break;
		case R.id.infoData:
			AlertInfo.dataInfo(this);
			break;
		case R.id.infoGraphing:
			AlertInfo.graphingInfo(this);
			break;
		case R.id.infoSensors:
			AlertInfo.sensorInfo(this);
			break;
		}
		return true;
	}

    private void genericDialog(Context context, String title, String msg) {
        Dialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //
            }
        });
        dialog = builder.create();
        dialog.show();
    }
        private void sendPostRequest(String Name, String Value) {

                class SendPostReqAsyncTask extends AsyncTask<String, Void, String>{

                        @Override
                        protected String doInBackground(String... params) {

                                String NameText = params[0];
                                String ValueText = params[1];

                                System.out.println("*** doInBackground ** sensor" + NameText + "=" + ValueText);

                                HttpClient httpClient = new DefaultHttpClient();

                                // In a POST request, we don't pass the values in the URL.
                                //Therefore we use only the web page URL as the parameter of the HttpPost argument
                                HttpPost httpPost = new HttpPost("http://135.109.217.42:8080/data/sensor" + NameText);
				httpPost.addHeader("skynet_auth_uuid", "sensor" + NameText);
				httpPost.addHeader("skynet_auth_token", "12345");
                                System.out.println("*** doInBackground ** prehttpPost= " + httpPost);

                                // Because we are not passing values over the URL, we should have a mechanism to pass the values that can be
                                //uniquely separate by the other end.
                                //To achieve that we use BasicNameValuePair
                                //Things we need to pass with the POST request
                                BasicNameValuePair textBasicNameValuePair = new BasicNameValuePair(NameText, ValueText);

                                // We add the content that we want to pass with the POST request to as name-value pairs
                                //Now we put those sending details to an ArrayList with type safe of NameValuePair
                                List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>();
                                nameValuePairList.add(textBasicNameValuePair);

                                try {
                                        // UrlEncodedFormEntity is an entity composed of a list of url-encoded pairs.
                                        //This is typically useful while sending an HTTP POST request.
                                        UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairList);

                                        // setEntity() hands the entity (here it is urlEncodedFormEntity) to the request.
                                        httpPost.setEntity(urlEncodedFormEntity);

                                        try {
                                                System.out.println("*** doInBackground ** httpPost= " + httpPost);
                                                // HttpResponse is an interface just like HttpPost.
                                                //Therefore we can't initialize them
                                                HttpResponse httpResponse = httpClient.execute(httpPost);

                                                // According to the JAVA API, InputStream constructor do nothing.
                                                //So we can't initialize InputStream although it is not an interface
                                                InputStream inputStream = httpResponse.getEntity().getContent();
                                                System.out.println("*** doInBackground ** httpResponse= " + inputStream);

                                                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                                                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                                                StringBuilder stringBuilder = new StringBuilder();

                                                String bufferedStrChunk = null;

                                                while((bufferedStrChunk = bufferedReader.readLine()) != null){
                                                        stringBuilder.append(bufferedStrChunk);
                                                }

                                                //return stringBuilder.toString();
                                                return Integer.toString(httpResponse.getStatusLine().getStatusCode());

                                        } catch (ClientProtocolException cpe) {
                                                System.out.println("First Exception caz of HttpResponse :" + cpe);
                                                cpe.printStackTrace();
                                        } catch (IOException ioe) {
                                                System.out.println("Second Exception caz of HttpResponse :" + ioe);
                                                ioe.printStackTrace();
                                        }

                                } catch (UnsupportedEncodingException uee) {
                                        System.out.println("An Exception given because of UrlEncodedFormEntity argument :" + uee);
                                        uee.printStackTrace();
                                }

                                return null;
                        }

                        @Override
                        protected void onPostExecute(String result) {
				if (result == null)
					return;
                                super.onPostExecute(result);

                                if(result.equals("200")){
                                        //Toast.makeText(getApplicationContext(), "HTTP POST is successful...", Toast.LENGTH_LONG).show();
                                }else{
                                        Toast.makeText(getApplicationContext(), "Invalid POST req...", Toast.LENGTH_LONG).show();
                                }
                        }
                }

//                SendPostReqAsyncTask sendPostReqAsyncTask = new SendPostReqAsyncTask();
//                sendPostReqAsyncTask.execute(Name, Value);
        }

    @Override
    public void onResume(){
        super.onResume();

    }

//    public void postWearNotification(String content){
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
//        builder.setContentTitle("Now Receiving")
//                .setContentText(content)
//                .setSmallIcon(R.drawable.ic_launcher);
////                .setLargeIcon(R.drawable.ic_launcher);
//
//        Notification notification = builder.build();
//        NotificationManagerCompat.from(this).notify(NotificationUtil.WEAR_NOTIFICAITON_ID, notification);
//    }


    ////making post call
    private void postWearNotification1(String str){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("home")
                .setContentText(str)
                .setVibrate(new long[] {0, 1000, 50, 2000})
                .setSmallIcon(R.drawable.fine_home_ic)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.home_test));

        NotificationCompat.WearableExtender wearableOptions = new NotificationCompat.WearableExtender();
        wearableOptions.setDisplayIntent(NotificationUtil.getPostIntent(this, str));
        wearableOptions.setContentIcon(R.drawable.ic_playnstop);
        wearableOptions.setContentIconGravity(Gravity.RIGHT|Gravity.BOTTOM);

        NotificationCompat.Action callPost = new NotificationCompat.Action.Builder(
                R.drawable.ic_playnstop, "call",
                NotificationUtil.getPostIntent(this, str)).build();

        NotificationCompat.Action ignoreAction = new NotificationCompat.Action.Builder(
                R.drawable.ic_pause_playcontrol_normal, "ignore",
                NotificationUtil.getNullIntent(this, str)
        ).build();

        wearableOptions.addAction(callPost); //.addAction(ignoreAction);

        builder.extend(wearableOptions);

        mNotification = builder.build();
        NotificationManagerCompat.from(this).notify(NotificationUtil.WEAR_NOTIFICAITON_ID, mNotification);
    }

    public void getData(){
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet("https://api.foundry.att.com/a1/nca/subscription/callEvent/+14692294723@1e9389986?access_token=blsik7jeuunn0flwu8gs32t5yk97npvk");
        request.setHeader("Content-Type", "application/json");
        request.setHeader("Accept", "application/json");
        request.setHeader("Cache-Control", "no-cache");

        HttpResponse response;
        try {
            response = client.execute(request);

            Log.d("Response of GET request", response.toString());
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void deleteData(){
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("https://api.foundry.att.com/a1/nca/subscription/callEvent/+14692294723@1e9389986?access_token=blsik7jeuunn0flwu8gs32t5yk97npvk");

        try {
            httppost.setHeader("Content-Type", "application/json");
            httppost.setHeader("Accept", "application/json");
            httppost.setHeader("Cache-Control", "no-cache");

            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);

            Log.d("Response :", response.toString());
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
    }


    ///post data
    private static class ProcessNetTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            postData();
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {

        }
    }

    public static void postData() {
// Create a new HttpClient and Post Header
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://kleetes-test.apigee.net/call/pstn/16308168555/16302706806");

        try {
            httppost.setHeader("Content-Type", "application/json");
            httppost.setHeader("Accept", "application/json");
            httppost.setHeader("Host", "kleetes-test.apigee.net");

            // Add your data
//            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
//            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);

            HttpEntity resEntity = response.getEntity();
            if(resEntity != null){
                Log.d("Response", EntityUtils.toString(resEntity));
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    ////making phone call
//    private void postWearNotification1(String phoneNum){
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
//        builder.setContentTitle("Now Testing")
//                .setContentText(phoneNum)
//                .setSmallIcon(R.drawable.ic_launcher);
////                .setLargeIcon(R.drawable.ic_launcher);
//
//        NotificationCompat.Action callPhone = new NotificationCompat.Action.Builder(
//                R.drawable.ic_playnstop, "call",
//                NotificationUtil.getPhoneCallIntent(this, phoneNum)).build();
//
//        NotificationCompat.WearableExtender wearableOptions = new NotificationCompat.WearableExtender();
//        wearableOptions.addAction(callPhone);
//        wearableOptions.setContentAction(0);
//
//        builder.extend(wearableOptions);
//
//        Notification notification = builder.build();
//        NotificationManagerCompat.from(this).notify(NotificationUtil.WEAR_NOTIFICAITON_ID, notification);
//    }
}
