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

package id.satusatudua.sigap.presenter;

import android.os.Bundle;

import id.satusatudua.sigap.data.api.FirebaseApi;
import id.satusatudua.sigap.data.local.CacheManager;
import id.satusatudua.sigap.data.local.StateManager;
import id.satusatudua.sigap.data.model.User;
import id.zelory.benih.presenter.BenihPresenter;
import id.zelory.benih.util.BenihScheduler;
import timber.log.Timber;

/**
 * Created on : December 22, 2015
 * Author     : zetbaitsu
 * Name       : Zetra
 * Email      : zetra@mail.ugm.ac.id
 * GitHub     : https://github.com/zetbaitsu
 * LinkedIn   : https://id.linkedin.com/in/zetbaitsu
 */
public class CurrentUserPresenter extends BenihPresenter<CurrentUserPresenter.View> {

    private User currentUser;

    public CurrentUserPresenter(View view) {
        super(view);
        listenCurrentUser();
    }

    private void listenCurrentUser() {
        CacheManager.pluck().listenCurrentUser()
                .compose(BenihScheduler.pluck().applySchedulers(BenihScheduler.Type.IO))
                .subscribe(user -> {
                    if (user != null) {
                        currentUser = user;
                        if (view != null) {
                            view.onCurrentUserChanged(currentUser);
                        }
                    }
                }, throwable -> {
                    Timber.e(throwable.getMessage());
                    if (view != null) {
                        view.showError(throwable.getMessage());
                    }
                });
    }

    public void logout() {
        view.showLoading();
        currentUser.setFromApps(false);
        FirebaseApi.pluck()
                .users(currentUser.getUserId())
                .setValue(currentUser, (firebaseError, firebase) -> {
                    if (firebaseError != null) {
                        Timber.d(firebaseError.getMessage());
                        if (view != null) {
                            view.showError(firebaseError.getMessage());
                            view.dismissLoading();
                        }
                    } else {
                        FirebaseApi.pluck().getApi().unauth();
                        StateManager.pluck().setState(StateManager.State.LOGOUT);
                        currentUser.setFromApps(false);
                        CacheManager.pluck().cacheCurrentUser(currentUser);
                        if (view != null) {
                            view.onSuccessLogout();
                            view.dismissLoading();
                        }
                    }
                });
    }

    @Override
    public void saveState(Bundle bundle) {

    }

    @Override
    public void loadState(Bundle bundle) {

    }

    public interface View extends BenihPresenter.View {

        void onCurrentUserChanged(User currentUser);

        void onSuccessLogout();
    }
}
