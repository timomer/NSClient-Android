package info.nightscout.nsupload;


import java.util.Date;

public class TempBasal {
    public long getTimeIndex() {
        return (long) Math.ceil(timeStart.getTime() / 60000d );
    }

    public void setTimeIndex(long timeIndex) {
        this.timeIndex = timeIndex;
    }

    public long timeIndex;


    public Date timeStart;

    public Date timeEnd;

    public int percent;

    public int duration;

    public int baseRatio;

    public int tempRatio;

    @Override
    public String toString() {
        return "TempBasal{" +
                "timeIndex=" + timeIndex +
                ", timeStart=" + timeStart +
                ", timeEnd=" + timeEnd +
                ", percent=" + percent +
                ", duration=" + duration +
                ", baseRatio=" + baseRatio +
                ", tempRatio=" + tempRatio +
                '}';
    }
}
