/*
 * Copyright (c) 2015 SatuSatuDua.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package id.satusatudua.sigap.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;

import com.firebase.client.DataSnapshot;

import java.util.List;

import id.satusatudua.sigap.R;
import id.satusatudua.sigap.SigapApp;
import id.satusatudua.sigap.data.api.FirebaseApi;
import id.satusatudua.sigap.data.local.CacheManager;
import id.satusatudua.sigap.data.local.StateManager;
import id.satusatudua.sigap.data.model.Case;
import id.satusatudua.sigap.data.model.User;
import id.satusatudua.sigap.ui.ConfirmGuardingActivity;
import id.satusatudua.sigap.ui.ConfirmHelpingActivity;
import id.satusatudua.sigap.ui.ConfirmTrustedOfActivity;
import id.satusatudua.sigap.ui.GuardingActivity;
import id.satusatudua.sigap.ui.HelpingActivity;
import id.satusatudua.sigap.util.RxFirebase;
import id.zelory.benih.util.BenihUtils;
import timber.log.Timber;

/**
 * Created on : January 13, 2016
 * Author     : zetbaitsu
 * Name       : Zetra
 * Email      : zetra@mail.ugm.ac.id
 * GitHub     : https://github.com/zetbaitsu
 * LinkedIn   : https://id.linkedin.com/in/zetbaitsu
 */
public class NotificationService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.tag(getClass().getSimpleName());
        Timber.d(getClass().getSimpleName() + " is creating");

        listenEmergency();
        listenEscortRequest();
        listenTrustedOf();
        listenNewMessage();
        listenNewGuardingMessage();
    }

    private void listenEscortRequest() {
        RxFirebase.observeChildAdded(FirebaseApi.pluck().userGuards(CacheManager.pluck().getCurrentUser().getUserId()))
                .map(firebaseChildEvent -> firebaseChildEvent.snapshot)
                .filter(dataSnapshot -> dataSnapshot.hasChild("delivered"))
                .filter(dataSnapshot -> !dataSnapshot.child("delivered").getValue(Boolean.class))
                .map(DataSnapshot::getKey)
                .doOnNext(escortId -> FirebaseApi.pluck()
                        .userGuards(CacheManager.pluck().getCurrentUser().getUserId())
                        .child(escortId)
                        .child("delivered")
                        .setValue(true)
                )
                .subscribe(escortId -> {
                    if (StateManager.pluck().getState() == StateManager.State.LOGGED) {
                        List<String> lastEscortIds = CacheManager.pluck().getLastGuarding();
                        if (lastEscortIds == null || !lastEscortIds.contains(escortId)) {
                            CacheManager.pluck().cacheGuarding(escortId);
                            showEscortRequestNotification(escortId);
                        }
                    }
                }, throwable -> Timber.e(throwable.getMessage()));
    }

    private void listenEmergency() {
        if (CacheManager.pluck().getUserLocation() != null) {
            RxFirebase.observeChildAdded(FirebaseApi.pluck().userHelps(CacheManager.pluck().getCurrentUser().getUserId()))
                    .map(firebaseChildEvent -> firebaseChildEvent.snapshot)
                    .filter(dataSnapshot -> dataSnapshot.hasChild("delivered"))
                    .filter(dataSnapshot -> !dataSnapshot.child("delivered").getValue(Boolean.class))
                    .map(DataSnapshot::getKey)
                    .doOnNext(caseId -> FirebaseApi.pluck()
                            .userHelps(CacheManager.pluck().getCurrentUser().getUserId())
                            .child(caseId)
                            .child("delivered")
                            .setValue(true)
                    )
                    .subscribe(caseId -> {
                        if (StateManager.pluck().getState() == StateManager.State.LOGGED) {
                            List<String> lastCaseIds = CacheManager.pluck().getLastCases();
                            if (lastCaseIds == null || !lastCaseIds.contains(caseId)) {
                                CacheManager.pluck().cacheCase(caseId);
                                //showEmergencyNotification(caseId);
                                startActivity(ConfirmHelpingActivity.generateIntent(this, caseId));
                            }
                        }
                    }, throwable -> Timber.e(throwable.getMessage()));
        }
    }

    private void showEscortRequestNotification(String escortId) {
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                                                                ConfirmGuardingActivity.generateIntent(this, escortId),
                                                                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle("Sigap")
                .setContentText("Seseorang meminta mu untuk mengawalnya!")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setVibrate(CacheManager.pluck().isVibrate() ? new long[]{100, 300, 500, 1000} : new long[]{})
                .setSound(Uri.parse(CacheManager.pluck().getRingtone()))
                .setAutoCancel(true)
                .setStyle(new android.support
                        .v4.app.NotificationCompat
                        .BigTextStyle()
                                  .bigText("Seseorang meminta mu untuk mengawalnya!"))
                .setContentIntent(pendingIntent)
                .build();

        NotificationManagerCompat
                .from(SigapApp.pluck().getApplicationContext())
                .notify(BenihUtils.randInt(1, Integer.MAX_VALUE), notification);
    }

    private void showEmergencyNotification(String caseId) {
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                                                                ConfirmHelpingActivity.generateIntent(this, caseId),
                                                                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle("Sigap")
                .setContentText("Seseorang membutuhkan bantuan mu!!!")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setVibrate(CacheManager.pluck().isVibrate() ? new long[]{100, 300, 500, 1000} : new long[]{})
                .setSound(Uri.parse(CacheManager.pluck().getRingtone()))
                .setAutoCancel(true)
                .setStyle(new android.support
                        .v4.app.NotificationCompat
                        .BigTextStyle()
                                  .bigText("Seseorang membutuhkan bantuan mu!!!, Ayo segera bantu dia!"))
                .setContentIntent(pendingIntent)
                .build();

        NotificationManagerCompat
                .from(SigapApp.pluck().getApplicationContext())
                .notify(BenihUtils.randInt(1, Integer.MAX_VALUE), notification);
    }

    private void listenNewMessage() {
        User currentUser = CacheManager.pluck().getCurrentUser();
        CacheManager.pluck()
                .listenLastHelpingCase()
                .filter(theCase -> theCase.getStatus() != Case.Status.DITUTUP)
                .flatMap(theCase -> RxFirebase.observeChildAdded(FirebaseApi.pluck().caseMessages(theCase.getCaseId()))
                        .map(firebaseChildEvent -> firebaseChildEvent.snapshot))
                .filter(dataSnapshot -> dataSnapshot.child("date").getValue(Long.class) > CacheManager.pluck().getLastMessageTime())
                .filter(dataSnapshot -> !dataSnapshot.child("userId").getValue().toString().equals(currentUser.getUserId()))
                .doOnNext(dataSnapshot -> CacheManager.pluck().cacheLastMessageTime(dataSnapshot.child("date").getValue(Long.class)))
                .subscribe(dataSnapshot -> showNewMessageNotif(dataSnapshot, false), throwable -> Timber.e(throwable.getMessage()));
    }

    private void listenNewGuardingMessage() {
        User currentUser = CacheManager.pluck().getCurrentUser();
        CacheManager.pluck()
                .listenLastGuardingEscort()
                .filter(escort -> !escort.isClosed())
                .flatMap(escort -> RxFirebase.observeChildAdded(FirebaseApi.pluck().escortMessages(escort.getEscortId()))
                        .map(firebaseChildEvent -> firebaseChildEvent.snapshot))
                .filter(dataSnapshot -> dataSnapshot.child("date").getValue(Long.class) > CacheManager.pluck().getLastMessageTime())
                .filter(dataSnapshot -> !dataSnapshot.child("userId").getValue().toString().equals(currentUser.getUserId()))
                .doOnNext(dataSnapshot -> CacheManager.pluck().cacheLastMessageTime(dataSnapshot.child("date").getValue(Long.class)))
                .subscribe(dataSnapshot -> showNewMessageNotif(dataSnapshot, true), throwable -> Timber.e(throwable.getMessage()));
    }

    private void showNewMessageNotif(DataSnapshot dataSnapshot, boolean isGuarding) {
        if (!BenihUtils.isMyAppRunning(getApplicationContext(), getPackageName())) {
            String content = dataSnapshot.child("content").getValue().toString();
            boolean danger = false;
            if (content.startsWith("[DANGER]") && content.endsWith("[/DANGER]")) {
                danger = true;
                content = content.replace("[DANGER]", "").replace("[/DANGER]", "");
            } else if (content.startsWith("[CLOSED]") && content.endsWith("[/CLOSED]")) {
                content = content.replace("[CLOSED]", "").replace("[/CLOSED]", "");
            } else if (content.startsWith("[INITIAL]") && content.endsWith("[/INITIAL]")) {
                content = content.replace("[INITIAL]", "").replace("[/INITIAL]", "");
            }
            PendingIntent pendingIntent;
            if (isGuarding) {
                pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                                                          GuardingActivity.generateIntent(this),
                                                          PendingIntent.FLAG_UPDATE_CURRENT);
            } else {
                pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                                                          HelpingActivity
                                                                  .generateIntent(this,
                                                                                  CacheManager.pluck().getLastHelpingCase(),
                                                                                  CacheManager.pluck().getLastCaseReporter()),
                                                          PendingIntent.FLAG_UPDATE_CURRENT);
            }
            Notification notification = new NotificationCompat.Builder(getApplicationContext())
                    .setContentTitle("Sigap")
                    .setContentText(danger ? content : "Seseorang mengirimkan pesan baru kedalam percakapan!")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .setVibrate(CacheManager.pluck().isVibrate() ? new long[]{100, 300, 500, 1000} : new long[]{})
                    .setSound(Uri.parse(CacheManager.pluck().getRingtone()))
                    .setAutoCancel(true)
                    .setStyle(new android.support
                            .v4.app.NotificationCompat
                            .BigTextStyle()
                                      .bigText(danger ? content :
                                                       "Seseorang mengirimkan pesan baru kedalam percakapan\nIsi pesan: " + content))
                    .setContentIntent(pendingIntent)
                    .build();

            NotificationManagerCompat
                    .from(SigapApp.pluck().getApplicationContext())
                    .notify(396, notification);
        }
    }

    private void listenTrustedOf() {
        RxFirebase.observeChildAdded(FirebaseApi.pluck().trustedOf(CacheManager.pluck().getCurrentUser().getUserId()))
                .map(firebaseChildEvent -> firebaseChildEvent.snapshot)
                .filter(dataSnapshot -> dataSnapshot.child("status").getValue().equals("MENUNGGU"))
                .map(DataSnapshot::getKey)
                .subscribe(this::showNotification, throwable -> Timber.e(throwable.getMessage()));
    }

    private void showNotification(String userId) {
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                                                                ConfirmTrustedOfActivity.generateIntent(this, userId),
                                                                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle("Sigap")
                .setContentText("Seseorang menambahkanmu kedalam kontak terpercayanya!")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setVibrate(CacheManager.pluck().isVibrate() ? new long[]{100, 300, 500, 1000} : new long[]{})
                .setSound(Uri.parse(CacheManager.pluck().getRingtone()))
                .setAutoCancel(true)
                .setStyle(new android.support
                        .v4.app.NotificationCompat
                        .BigTextStyle()
                                  .bigText("Seseorang menambahkanmu kedalam kontak terpercayanya!, klik untuk mengkonfirmasi."))
                .setContentIntent(pendingIntent)
                .build();

        NotificationManagerCompat
                .from(SigapApp.pluck().getApplicationContext())
                .notify(BenihUtils.randInt(1, Integer.MAX_VALUE), notification);
    }
}
