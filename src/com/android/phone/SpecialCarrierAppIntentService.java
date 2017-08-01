package com.android.phone;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Slog;

import androidx.core.app.NotificationCompat;

import com.android.internal.telephony.SpecialCarrierAppInfo;

public class SpecialCarrierAppIntentService extends IntentService {

    private static final String TAG = "SpecialCarrierApp";

    private static final boolean DEBUG = false; // STOPSHIP if true

    /**
     * Action to handle a match.
     * The app will be enabled if it matched while the device was not provisioned, otherwise
     * a notification will be displayed to let the user enable the app (or not).
     */
    public static final String ACTION_HANDLE_MATCH = "com.android.phone.SpecialCarrierApp.handle_match";
    /**
     * Action to dismiss the offer to enable/disable the carrier app (will be offered again).
     */
    public static final String ACTION_DISMISS_OFFER = "com.android.phone.SpecialCarrierApp.dismiss_notification";
    /**
     * Action to enable the app.
     */
    public static final String ACTION_ENABLE_APP = "com.android.phone.SpecialCarrierApp.enable_app";
    /**
     * Action to disable the app.
     */
    public static final String ACTION_DISABLE_APP = "com.android.phone.SpecialCarrierApp.disable_app";

    public static final String EXTRA_APP_INFO = "com.android.phone.SpecialCarrierApp.app_info";
    public static final String EXTRA_DEVICE_PROVISIONED = "com.android.phone.SpecialCarrierApp.device_provisioned";

    private IPackageManager mPackageManager;

    public SpecialCarrierAppIntentService() {
        super("SpecialCarrierAppIntentService");

        mPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    }

    public static PendingIntent getPendingDismissIntent(Context context, SpecialCarrierAppInfo appInfo) {
        final Intent intent = new Intent(context, SpecialCarrierAppIntentService.class)
                .setAction(ACTION_DISMISS_OFFER)
                .putExtra(EXTRA_APP_INFO, appInfo);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
    }

    public static PendingIntent getPendingEnableIntent(Context context, SpecialCarrierAppInfo appInfo) {
        final Intent intent = new Intent(context, SpecialCarrierAppIntentService.class)
                .setAction(ACTION_ENABLE_APP)
                .putExtra(EXTRA_APP_INFO, appInfo);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
    }

    public static PendingIntent getPendingDisableIntent(Context context, SpecialCarrierAppInfo appInfo) {
        final Intent intent = new Intent(context, SpecialCarrierAppIntentService.class)
                .setAction(ACTION_DISABLE_APP)
                .putExtra(EXTRA_APP_INFO, appInfo);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (DEBUG) Slog.d(TAG, "Received intent with action: " + action);

            final Object appInfoObject = intent.getSerializableExtra(EXTRA_APP_INFO);
            if (appInfoObject == null || !(appInfoObject instanceof SpecialCarrierAppInfo)) {
                Slog.wtf(TAG, "EXTRA_APP_INFO is mandatory");
                return;
            }
            final SpecialCarrierAppInfo appInfo = (SpecialCarrierAppInfo) appInfoObject;

            if (ACTION_DISMISS_OFFER.equals(action)) {
                handleActionDismissOffer(appInfo);
            } else if (ACTION_ENABLE_APP.equals(action)) {
                handleActionEnableApp(appInfo);
            } else if (ACTION_DISABLE_APP.equals(action)) {
                handleActionDisableApp(appInfo);
            } else if (ACTION_HANDLE_MATCH.equals(action)) {
                handleActionHandleMatch(appInfo,
                        intent.getBooleanExtra(EXTRA_DEVICE_PROVISIONED, true));
            } else {
                Slog.w(TAG, "Unknown action: " + action);
            }
        }
    }

    private void handleActionDismissOffer(SpecialCarrierAppInfo appInfo) {
        Slog.d(TAG, "Dismissed, the special carrier app " + appInfo.mPackageName + " will be offered again.");
    }

    private void handleActionHandleMatch(SpecialCarrierAppInfo appInfo, boolean wasDeviceProvisioned) {
        if (!wasDeviceProvisioned) {
            if (DEBUG) Slog.d(TAG, "The device was not yet provisioned when the special carrier app matched.");

            enableSpecialCarrierApp(appInfo);
        } else {
            if (DEBUG) Slog.d(TAG, "The device was already provisioned when the special carrier app matched.");

            showNotification(appInfo);
        }
    }

    private void handleActionEnableApp(SpecialCarrierAppInfo appInfo) {
        enableSpecialCarrierApp(appInfo);
        cancelNotification(appInfo);
    }

    private void handleActionDisableApp(SpecialCarrierAppInfo appInfo) {
        disableSpecialCarrierApp(appInfo);
        cancelNotification(appInfo);
    }

    private void showNotification(SpecialCarrierAppInfo appInfo) {
        createNotificationChannel(getApplicationContext());
        final int slotNumberToDisplay = appInfo.mUiccSlotId + 1;

        if (TextUtils.isEmpty(appInfo.mCarrierName)) {
            appInfo.mCarrierName = getString(R.string.special_carrier_app_generic_carrier_name);
        }

        final String channelId = getString(R.string.notification_channel_id);
        final Notification notification =
                new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setDeleteIntent(getPendingDismissIntent(getApplicationContext(), appInfo))
                .setContentTitle(getString(R.string.special_carrier_app_for_uicc, slotNumberToDisplay))
                .setContentText(getString(R.string.special_carrier_app_summary, appInfo.mCarrierName))
                .setSmallIcon(com.android.internal.R.drawable.ic_sim_card_multi_48px_clr)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        getString(R.string.special_carrier_app_summary_extended, appInfo.mCarrierName, slotNumberToDisplay)))
                .addAction(
                        new NotificationCompat.Action.Builder(
                                R.drawable.ic_special_carrier_app_disable,
                                getString(R.string.special_carrier_app_disable),
                                getPendingDisableIntent(getApplicationContext(), appInfo)).build())
                .addAction(
                        new NotificationCompat.Action.Builder(
                                R.drawable.ic_special_carrier_app_enable,
                                getString(R.string.special_carrier_app_enable),
                                getPendingEnableIntent(getApplicationContext(), appInfo)).build())
                .build();

        ((NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE)
        ).notify(appInfo.mUiccSlotId, notification);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = context.getString(R.string.notification_channel_id);
            CharSequence name = context.getString(R.string.notification_channel_name);
            String description = context.getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            ((NotificationManager) getApplicationContext()
                    .getSystemService(Context.NOTIFICATION_SERVICE)
            ).createNotificationChannel(channel);
        }
    }

    private void cancelNotification(SpecialCarrierAppInfo appInfo) {
        ((NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE)
        ).cancel(appInfo.mUiccSlotId);
    }

    private void enableSpecialCarrierApp(SpecialCarrierAppInfo appInfo) {
        updateCarrierAppState(appInfo, true);
        if (appInfo.mActionIntentToBroadcast != null) {
            getApplicationContext().sendBroadcast(new Intent(appInfo.mActionIntentToBroadcast));
        }
    }

    private void disableSpecialCarrierApp(SpecialCarrierAppInfo appInfo) {
        updateCarrierAppState(appInfo, false);
    }

    private void updateCarrierAppState(SpecialCarrierAppInfo appInfo, boolean enable) {
        Slog.i(TAG, "Update state(" + appInfo.mPackageName + "): " + (enable ? "ENABLED" : "DISABLED")
                + " for user " + getApplicationContext().getUserId());
        try {
            mPackageManager.setApplicationEnabledSetting(appInfo.mPackageName,
                    enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
                    enable ? PackageManager.DONT_KILL_APP : 0, getApplicationContext().getUserId(),
                    getApplicationContext().getOpPackageName());
        } catch (RemoteException e) {
            Slog.w(TAG, "Could not reach PackageManager", e);
        }
    }
}
