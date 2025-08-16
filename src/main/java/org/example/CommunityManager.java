package org.example;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.*;
import java.util.*;

public class CommunityManager {
    private final Map<Long, String> members = new LinkedHashMap<>();
    private static final String MEMBERS_FILE = "community_members.json";

    public boolean addMember(long id, String name) {
        if (members.containsKey(id)) return false;
        members.put(id, name);
        saveMembers();
        return true;
    }
    public int getSize() { return members.size(); }
    public Set<Long> getAllMemberIds() { return members.keySet(); }
    public String getMemberName(long id) { return members.getOrDefault(id, "Unknown"); }
    public boolean hasEnoughMembers() { return members.size() >= 3; }

    public void saveMembers() {
        JSONObject json = new JSONObject();
        for (var e : members.entrySet()) json.put(String.valueOf(e.getKey()), e.getValue());
        try (FileWriter fw = new FileWriter(MEMBERS_FILE)) {
            fw.write(json.toString(2));
        } catch (IOException e) {
            System.err.println("Failed to save: " + e.getMessage());
        }
    }

    public void loadMembers() {
        File f = new File(MEMBERS_FILE);
        if (!f.exists()) return;
        try (FileReader fr = new FileReader(f)) {
            JSONObject json = new JSONObject(new JSONTokener(fr));
            members.clear();
            for (String k : json.keySet()) {
                members.put(Long.parseLong(k), json.getString(k));
            }
        } catch (Exception e) {
            System.err.println("Failed to load: " + e.getMessage());
        }
    }
}
