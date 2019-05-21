package com.example.smartiot;

import com.google.gson.annotations.SerializedName;

public class FeedsModel {

    @SerializedName("created_at")
    public String created_at;

    @SerializedName("entry_id")
    public int entry_id;

    @SerializedName("field1")
    public int field1;

    @SerializedName("field2")
    public int field2;


}
