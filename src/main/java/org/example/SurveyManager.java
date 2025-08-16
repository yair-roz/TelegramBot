package org.example;
import java.util.*;

public class SurveyManager {
    private Survey activeSurvey = null;
    private SurveyClosedListener listener;
    private VoteUpdateListener voteListener;

    public interface SurveyClosedListener {
        void onSurveyClosed(Survey survey);
    }

    public interface VoteUpdateListener {
        void onVoteUpdated(Survey survey);
    }

    public void setSurveyClosedListener(SurveyClosedListener listener) {
        this.listener = listener;
    }

    public void setVoteUpdateListener(VoteUpdateListener l) {
        this.voteListener = l;
    }

    public synchronized boolean hasActiveSurvey() {
        return activeSurvey != null;
    }

    public synchronized boolean createSurvey(Survey survey) {
        if (hasActiveSurvey()) return false;
        this.activeSurvey = survey;
        return true;
    }

    public synchronized Survey getActiveSurvey() {
        return activeSurvey;
    }

    public synchronized void closeSurvey() {
        if (activeSurvey != null) {
            Survey closed = activeSurvey;
            activeSurvey = null;
            if (listener != null) listener.onSurveyClosed(closed);
        }
    }

    public void notifyVoteUpdated() {
        if (voteListener != null && activeSurvey != null) {
            voteListener.onVoteUpdated(activeSurvey);
        }
    }

    public void notifyVoteUpdated(Survey survey) {
        if (voteListener != null) voteListener.onVoteUpdated(survey);
    }
}