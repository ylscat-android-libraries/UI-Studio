package research.ui.date;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created at 2016/5/13.
 *
 * @author YinLanShan
 */
public class Main extends Activity implements CompoundButton.OnCheckedChangeListener {
    private DatePickerController mController;
    GregorianCalendar min, max;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        NumberPicker year = (NumberPicker) findViewById(R.id.year);
        NumberPicker month = (NumberPicker) findViewById(R.id.month);
        NumberPicker day = (NumberPicker) findViewById(R.id.day);
        NumberPicker hour = (NumberPicker) findViewById(R.id.hour);
        NumberPicker minute = (NumberPicker) findViewById(R.id.minute);

        CheckBox cb = (CheckBox) findViewById(R.id.limit);
        cb.setOnCheckedChangeListener(this);

        mController = new DatePickerController(
                year, month, day, hour, minute);

        min = new GregorianCalendar(2000, Calendar.OCTOBER, 21, 22, 12);
        max = new GregorianCalendar(2010, Calendar.FEBRUARY, 3, 7, 3);
        TextView tv = (TextView)findViewById(R.id.min);
        tv.setText(String.format("%1$tF %1$tT", min));
        tv = (TextView)findViewById(R.id.max);
        tv.setText(String.format("%1$tF %1$tT", max));
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked) {
            mController.setLimitation(min, max);
        }
        else {
            mController.setLimitation(null, null);
        }
    }
}
