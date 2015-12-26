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

package id.satusatudua.sigap.data.model;

import android.os.Parcel;

/**
 * Created on : December 26, 2015
 * Author     : zetbaitsu
 * Name       : Zetra
 * Email      : zetra@mail.ugm.ac.id
 * GitHub     : https://github.com/zetbaitsu
 * LinkedIn   : https://id.linkedin.com/in/zetbaitsu
 */
public class CoverMessage extends MessageGroup {
    private String userId;

    public CoverMessage() {

    }

    protected CoverMessage(Parcel in) {
        super(in);
        userId = in.readString();
    }

    public static final Creator<CoverMessage> CREATOR = new Creator<CoverMessage>() {
        @Override
        public CoverMessage createFromParcel(Parcel in) {
            return new CoverMessage(in);
        }

        @Override
        public CoverMessage[] newArray(int size) {
            return new CoverMessage[size];
        }
    };

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CoverMessage && ((CoverMessage) o).userId.equals(userId);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(userId);
    }

    @Override
    public String toString() {
        return "CoverMessage{" +
                "userId='" + userId + '\'' +
                ", messages=" + messages +
                '}';
    }
}
