package org.example;

public class CommunityManagerHolder {
    private static CommunityManager instance;
    public static void setInstance(CommunityManager cm) { instance = cm; }
    public static CommunityManager getInstance() { return instance; }
}
