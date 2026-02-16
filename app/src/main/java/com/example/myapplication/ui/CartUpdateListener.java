package com.example.myapplication.ui; // ή το κατάλληλο πακέτο σου

import java.util.ArrayList;

public interface CartUpdateListener {
    void onCartUpdated(ArrayList<MyObject> updatedCart);
}
