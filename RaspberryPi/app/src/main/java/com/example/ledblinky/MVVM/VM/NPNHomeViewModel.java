package com.example.ledblinky.MVVM.VM;

import com.example.ledblinky.MVVM.View.NPNHomeView;
import com.example.ledblinky.Network.ApiResponseListener;

/**
 * Created by Le Trong Nhan on 19/06/2018.
 */

public class NPNHomeViewModel extends BaseViewModel<NPNHomeView> {
    public void updateToServer(String url)
    {
        requestGETWithURL(url, new ApiResponseListener<String>() {
            @Override
            public void onSuccess(String response) {
                view.onSuccessUpdateServer(response);
            }

            @Override
            public void onError(String error) {
                view.onErrorUpdateServer(error);
            }
        });
    }
}
