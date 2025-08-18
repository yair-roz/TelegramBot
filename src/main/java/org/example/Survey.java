package org.example;
import java.util.*;

public class Survey {
    public static class Question {
        public final String text;
        public final List<String> options;
        private final Map<String, Integer> votes = new LinkedHashMap<>();
        private final Set<Long> voters = new HashSet<>();

        public Question(String text, List<String> options) {
            this.text = text;
            this.options = options;
            for (String o : options) votes.put(o, 0);
        }
        public boolean canVote(long userId) { return !voters.contains(userId); }
        public void markVoted(long userId)   { voters.add(userId); }
        public void vote(String option)      { votes.put(option, votes.get(option) + 1); }
        public Map<String, Integer> getResults() { return votes; }
    }

    public final List<Question> questions;
    public final long creatorId;
    public final long delayMillis;
    private final Set<Long> respondents = new HashSet<>();
    private final long createdAt = System.currentTimeMillis();

    public Survey(long creatorId, List<Question> qs, long delayMinutes) {
        this.creatorId   = creatorId;
        this.questions   = qs;
        this.delayMillis = delayMinutes * 60 * 1000L;
    }

    public boolean isOpen() {
        long age = System.currentTimeMillis() - createdAt;
        return age < delayMillis + 5 * 60 * 1000L && !allAnswered();
    }

    public boolean allAnswered() {
        int size = CommunityManagerHolder.getInstance().getSize();
        return respondents.size() >= size && size > 0;
    }

    public void markRespondent(long userId) { respondents.add(userId); }
}
