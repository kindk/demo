package Decoder;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by yjd on 2/22/17.
 */

public class StatelessRecognizer {
    private Spectrum spectrum;
    private Collection<Tone> tones;

    public StatelessRecognizer(Spectrum spectrum) {
        this.spectrum = spectrum;
        tones = new ArrayList<Tone>();
        fillTones();
    }

    private void fillTones() {
        tones.add(new Tone(45, 77, '1'));
        tones.add(new Tone(45, 86, '2'));
        tones.add(new Tone(45, 95, '3'));
        tones.add(new Tone(49, 77, '4'));
        tones.add(new Tone(49, 86, '5'));
        tones.add(new Tone(49, 95, '6'));
        tones.add(new Tone(55, 77, '7'));
        tones.add(new Tone(55, 86, '8'));
        tones.add(new Tone(55, 95, '9'));
        tones.add(new Tone(60, 77, '*'));
        tones.add(new Tone(60, 86, '0'));
        tones.add(new Tone(60, 95, '#'));
        //Add by yjd on 01-05-2017.
        tones.add(new Tone(46, 107, 'A')); //45 -> 46, 104 -> 107
        tones.add(new Tone(51, 107, 'B')); //49 -> 51
        tones.add(new Tone(55, 107, 'C')); //104 -> 107
        tones.add(new Tone(60, 107, 'D')); // 104->107
    }

    public char getRecognizedKey() {
        SpectrumFragment lowFragment= new SpectrumFragment(0, 75, spectrum);
        SpectrumFragment highFragment= new SpectrumFragment(75, 150, spectrum);

        int lowMax = lowFragment.getMax();
        int highMax = highFragment.getMax();

        SpectrumFragment allSpectrum = new SpectrumFragment(0, 150, spectrum);
        int max = allSpectrum.getMax();

        //Log.i("StatelessRecognizer", "low high max: " + lowMax + ' ' + highMax + ' ' + max);
        if(max != lowMax && max != highMax)
            return ' ';

        for (Tone t : tones) {
            if(t.match(lowMax, highMax))
                return t.getKey();
        }

        return ' ';
    }
}
