package com.pyramidsoft.siqasir.ui.kasir;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class KasirViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public KasirViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is kasir fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}
