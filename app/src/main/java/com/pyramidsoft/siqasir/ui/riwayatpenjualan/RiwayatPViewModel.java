package com.pyramidsoft.siqasir.ui.riwayatpenjualan;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RiwayatPViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public RiwayatPViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Profile fragment");
    }

    public LiveData<String> getText() {return mText;}
}
