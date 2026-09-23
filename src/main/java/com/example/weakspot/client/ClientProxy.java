package com.example.weakspot.client;

import com.example.weakspot.CommonProxy;

public class ClientProxy extends CommonProxy {

    @Override
    public void init() {
        StatsKeyHandler.register();
    }
}
