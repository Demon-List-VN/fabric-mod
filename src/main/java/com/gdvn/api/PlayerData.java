package com.gdvn.api;

public class PlayerData {
    public int id;
    public String name;
    public Integer clan;
    public ClanData clans;
    public String supporterUntil;

    public static class ClanData {
        public int id;
        public String tag;
        public String tagBgColor;
        public String tagTextColor;
        public String boostedUntil;
    }
}
