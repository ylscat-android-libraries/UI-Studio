package research.ui.date;

import android.widget.NumberPicker;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

class DatePickerController implements NumberPicker.OnValueChangeListener {
    private Calendar mCalendar;
    private NumberPicker mYear;
    private NumberPicker mMonth;
    private NumberPicker mDay;
    private NumberPicker mHour;
    private NumberPicker mMinute;
    Map<NumberPicker, Integer> mPickerIndex = new HashMap<>();

    private int[] mMin;
    private int[] mMax;
    private int[] mTime;
    private int mMinMatchCount;
    private int mMaxMatchCount;

    public DatePickerController(NumberPicker year, NumberPicker month, NumberPicker day,
                                NumberPicker hour, NumberPicker min) {
        Calendar c = mCalendar = Calendar.getInstance();
        mYear = year;
        mMonth = month;
        mDay = day;
        mHour = hour;
        mMinute = min;

        mPickerIndex.put(year, 0);
        mPickerIndex.put(month, 1);
        mPickerIndex.put(day, 2);
        mPickerIndex.put(hour, 3);
        mPickerIndex.put(min, 4);

        boolean full = hour != null;
        int[] now;
        if(full) {
            now = new int[5];
            now[0] = c.get(Calendar.YEAR);
            now[1] = c.get(Calendar.MONTH) + 1;
            now[2] = c.get(Calendar.DAY_OF_MONTH);
            now[3] = c.get(Calendar.HOUR_OF_DAY);
            now[4] = c.get(Calendar.MINUTE);
        }
        else {
            now = new int[3];
            now[0] = c.get(Calendar.YEAR);
            now[1] = c.get(Calendar.MONTH) + 1;
            now[2] = c.get(Calendar.DAY_OF_MONTH);
        }

        mTime = now;

        year.setMinValue(now[0] - 10);
        year.setMaxValue(now[0] + 10);
        month.setMinValue(1);
        month.setMaxValue(12);
        day.setMinValue(1);
        day.setMaxValue(c.getActualMaximum(Calendar.DAY_OF_MONTH));
        if(full) {
            hour.setMaxValue(23);
            min.setMaxValue(59);
        }

        year.setValue(now[0]);
        month.setValue(now[1]);
        day.setValue(now[2]);
        if(full) {
            hour.setValue(now[3]);
            min.setValue(now[4]);
        }

        year.setOnValueChangedListener(this);
        month.setOnValueChangedListener(this);
        day.setOnValueChangedListener(this);
        if(full) {
            hour.setOnValueChangedListener(this);
            min.setOnValueChangedListener(this);
        }
    }

    public DatePickerController(NumberPicker year, NumberPicker month, NumberPicker day) {
        this(year, month, day, null, null);
    }

    public void setLimitation(Calendar min, Calendar max) {
        if(min != null) {
            int[] t = new int[5];
            t[0] = min.get(Calendar.YEAR);
            t[1] = min.get(Calendar.MONTH) + 1;
            t[2] = min.get(Calendar.DAY_OF_MONTH);
            t[3] = min.get(Calendar.HOUR_OF_DAY);
            t[4] = min.get(Calendar.MINUTE);
            mYear.setMinValue(t[0]);
            mMin = t;
        }
        else {
            mMin = null;
            if(max != null)
                mYear.setMinValue(max.get(Calendar.YEAR) - 10);
            else
                mYear.setMinValue(Calendar.getInstance().get(Calendar.YEAR) - 10);
        }

        if(max != null) {
            int[] t = new int[5];
            t[0] = max.get(Calendar.YEAR);
            t[1] = max.get(Calendar.MONTH) + 1;
            t[2] = max.get(Calendar.DAY_OF_MONTH);
            t[3] = max.get(Calendar.HOUR_OF_DAY);
            t[4] = max.get(Calendar.MINUTE);
            mYear.setMaxValue(t[0]);
            mMax = t;
        }
        else {
            mMax = null;
            if(min != null)
                mYear.setMaxValue(min.get(Calendar.YEAR) + 10);
            else
                mYear.setMaxValue(Calendar.getInstance().get(Calendar.YEAR) + 10);
        }

        mMonth.setMinValue(1);
        mMonth.setMaxValue(12);
        mDay.setMinValue(1);
        if(mHour != null) {
            mHour.setMinValue(0);
            mHour.setMaxValue(23);
        }
        if(mMinute != null) {
            mMinute.setMinValue(0);
            mMinute.setMaxValue(59);
        }
        mMaxMatchCount = 0;
        mMinMatchCount = 0;
        onValueChange(mYear, mTime[0], mYear.getValue());
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        int index = mPickerIndex.get(picker);
        mTime[index] = newVal;

        if(mMin != null)
            checkLowerBound(mMin);

        if(mMax != null)
            checkUpperBound(mMax);

        //Adjust max day of month
        if (index < 2) {
            mCalendar.set(mTime[0], mTime[1] - 1, 1);
            int max = mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            if(mMax == null || mMaxMatchCount < 2)
                mDay.setMaxValue(max);
        }
    }

    private void checkLowerBound (int[] min) {
        int[] time = mTime;

        int matched;
        for (matched = 0; matched < time.length; matched++) {
            if (time[matched] != min[matched])
                break;
        }

        //release min
        for (int i = matched; i < mMinMatchCount; i++) {
            switch (i + 1) {
                case 1: //month
                    mMonth.setMinValue(1);
                    break;
                case 2: //day
                    mDay.setMinValue(1);
                    break;
                case 3: //hour
                    if(mHour != null)
                        mHour.setMinValue(0);
                    break;
                case 4: //min
                    mMinute.setMinValue(0);
                    break;
            }
        }

        //limit min
        for (int i = mMinMatchCount; i < matched; i++) {
            int index = i + 1;
            boolean limitMore = false;
            switch (index) {
                case 1: //month
                    mMonth.setMinValue(min[1]);
                    if (index == matched && min[1] == mMonth.getValue())
                        limitMore = true;
                    break;
                case 2: //day
                    mDay.setMinValue(min[2]);
                    if (index == matched && min[2] == mDay.getValue())
                        limitMore = true;
                    break;
                case 3: //hour
                    if(mHour != null) {
                        mHour.setMinValue(min[3]);
                        if (index == matched && min[3] == mHour.getValue())
                            limitMore = true;
                    }
                    break;
                case 4: //min
                    mMinute.setMinValue(min[4]);
                    if (index == matched && min[4] == mMinute.getValue())
                        limitMore = true;
                    break;
            }
            if (limitMore) {
                time[index] = min[index];
                matched++;
            }
        }

        mMinMatchCount = matched;
    }

    private void checkUpperBound (int[] max) {
        int[] time = mTime;

        int matched;
        for (matched = 0; matched < time.length; matched++) {
            if (time[matched] != max[matched])
                break;
        }

        //release max
        for (int i = matched; i < mMaxMatchCount; i++) {
            switch (i + 1) {
                case 1: //month
                    mMonth.setMaxValue(12);
                    break;
                case 2: //day
                    mCalendar.set(mTime[0], mTime[1] - 1, 1);
                    int maximum = mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
                    mDay.setMaxValue(maximum);
                    if (maximum < mTime[2])
                        mTime[2] = maximum;
                    break;
                case 3: //hour
                    if(mHour != null)
                        mHour.setMaxValue(23);
                    break;
                case 4: //min
                    mMinute.setMaxValue(59);
                    break;
            }
        }

        //limit max
        for (int i = mMaxMatchCount; i < matched; i++) {
            int index = i + 1;
            boolean limitMore = false;
            switch (index) {
                case 1: //month
                    mMonth.setMaxValue(max[1]);
                    if (index == matched && max[1] == mMonth.getValue())
                        limitMore = true;
                    break;
                case 2: //day
                    mDay.setMaxValue(max[2]);
                    if (index == matched && max[2] == mDay.getValue())
                        limitMore = true;
                    break;
                case 3: //hour
                    if(mHour != null) {
                        mHour.setMaxValue(max[3]);
                        if (index == matched && max[3] == mHour.getValue())
                            limitMore = true;
                    }
                    break;
                case 4: //min
                    mMinute.setMaxValue(max[4]);
                    if (index == matched && max[4] == mMinute.getValue())
                        limitMore = true;
                    break;
            }
            if (limitMore) {
                time[index] = max[index];
                matched++;
            }
        }

        mMaxMatchCount = matched;
    }

    public int[] getTime() {
        return mTime;
    }
}