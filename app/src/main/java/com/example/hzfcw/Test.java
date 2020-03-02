package com.example.hzfcw;
import com.google.gson.Gson;

public class Test {
    public static void main(String[] args) {
        BLEDevice bleDevice=new BLEDevice("hh","22");
        Gson gson = new Gson();
        String jsonDevice = gson.toJson(bleDevice);
        System.out.println(jsonDevice);
    }


    static class BLEDevice {
        String name;
        String address;

        public BLEDevice(String _name, String _address) {
            this.name = _name;
            this.address = _address;
        }
    }
}
