/*
 * This class creates a GUI with a 16x16 grid of check boxes, which represent 16
 * time intervals and 16 instruments. The user can select any amount of checkboxes
 * to create a musical beat. Furthermore the class also allows to save a certain beat
 * through serialization.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.sound.midi.*;
import java.util.*;
import java.io.*;

public class BeatBox implements Serializable{

	JPanel mainPanel;
	ArrayList<JCheckBox> checkboxList;
	Sequencer sequencer;
	Sequence sequence;
	Track track;
	JFrame theFrame;

	String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"};
	
	int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};


	public static void main(String[] args){
		new BeatBox().buildGUI();
	}

	// This method sets up the GUI where the beats can be created, played, and saved
	public void buildGUI(){
		theFrame = new JFrame("Cyber BeatBox");
		theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		BorderLayout layout = new BorderLayout();
		JPanel background = new JPanel(layout);
		background.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		checkboxList  = new ArrayList<JCheckBox>();
		Box buttonBox = new Box(BoxLayout.Y_AXIS);

		JButton start = new JButton("Start");
		start.addActionListener(new MyStartListener());
		buttonBox.add(start);

		JButton stop = new JButton("Stop");
		stop.addActionListener(new MyStopListener());
		buttonBox.add(stop);

		JButton upTempo = new JButton("Tempo Up");
		stop.addActionListener(new MyUpTempoListener());
		buttonBox.add(upTempo);

		JButton downTempo = new JButton("Tempo Down");
		stop.addActionListener(new MyDownTempoListener());
		buttonBox.add(downTempo);

		JButton reset = new JButton("Reset");
		reset.addActionListener(new MyResetListener());
		buttonBox.add(reset);

		JButton save = new JButton("Save");
		save.addActionListener(new saveListener());
		buttonBox.add(save);

		JButton restore = new JButton("Restore");
		restore.addActionListener(new restoreListener());
		buttonBox.add(restore);

		Box nameBox = new Box(BoxLayout.Y_AXIS);
		for (int i = 0; i < 16; i++){
			nameBox.add(new Label(instrumentNames[i]));
		}

		background.add(BorderLayout.EAST, buttonBox);
		background.add(BorderLayout.WEST, nameBox);

		theFrame.getContentPane().add(background);

		GridLayout grid = new GridLayout(16, 16);
		grid.setVgap(1);
		grid.setHgap(1);
		mainPanel = new JPanel(grid);
		background.add(BorderLayout.CENTER, mainPanel);

		for (int i = 0; i < 256; i++) {
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			checkboxList.add(c);
			mainPanel.add(c);
		}

		setUpMidi();

		theFrame.setBounds(50, 50, 300, 300);
		theFrame.pack();
		theFrame.setVisible(true);
	}

	// This method just sets up the sequencer
	public void setUpMidi(){
		try{
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ, 4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);;
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	// This method creates the track base on the curently selected checkboxes and starts playing it
	public void buildTrackAndStart(){
		int[] trackList = null;

		sequence.deleteTrack(track);
		track = sequence.createTrack();

		for (int i = 0; i < 16; i++){
			trackList = new int[16];

			int key = instruments[i];

			for(int j = 0; j < 16; j++){
				JCheckBox jc = (JCheckBox) checkboxList.get(j + (16*i));
				if( jc.isSelected()){
					trackList[j] = key;
				}
				else{
					trackList[j] = 0;
				}
			}

			makeTracks(trackList);
			track.add(makeEvent(176, 1, 127, 0, 16));

		}

		track.add(makeEvent(192, 9, 1, 0, 15));
		try{
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		}
		catch(Exception e){
			e.printStackTrace();
		}

	}


	// Inner class for the start button
	class MyStartListener implements ActionListener {
		public void actionPerformed(ActionEvent event){
			buildTrackAndStart();
		}
	}

	// Inner class for the stop button
	class MyStopListener implements ActionListener {
		public void actionPerformed(ActionEvent event){
			sequencer.stop();
		}
	}

	// Inner class for the tempo up button
	class MyUpTempoListener implements ActionListener {
		public void actionPerformed(ActionEvent event){
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float)(tempoFactor * 1.03));
		}
	}

	// Inner class for the tempo down button
	class MyDownTempoListener implements ActionListener {
		public void actionPerformed(ActionEvent event){
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float)(tempoFactor * .97));
		}
	}

	// Inner class that sets all the checkboxes to false
	class MyResetListener implements ActionListener{
		public void actionPerformed(ActionEvent event){
			for (int i = 0; i < 256; i++){
				checkboxList.get(i).setSelected(false);
			}
		}
	}

	// Inner class for the save button that saves the current beat through serialization
	class saveListener implements ActionListener{
		public void actionPerformed(ActionEvent event){
			boolean[] checkBoxState = new boolean[256];

			for(int i = 0; i < 256; i++){
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if(check.isSelected()){
					checkBoxState[i] = true;
				}
			}

			try{
				ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("CheckBox.ser"));
				oos.writeObject(checkBoxState);
			}
			catch(Exception e){
				System.out.println("Beat could not be saved");
				e.printStackTrace();
			}

		}
	}

	// Inner class for the restore button that restores a previously saved beat
	class restoreListener implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			boolean[] checkBoxState = null;

			try{
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream("CheckBox.ser"));
				checkBoxState = (boolean[]) ois.readObject();
			}
			catch(Exception ex){
				System.out.println("Beat could not be restored");
				ex.printStackTrace();
			}

			for (int i = 0; i < 256; i++){
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if(checkBoxState[i]){
					check.setSelected(true);
				}
				else{
					check.setSelected(false);
				}
			}

			sequencer.stop();
			buildTrackAndStart();

		}
	}


	public void makeTracks(int[] list) {
		for(int i = 0; i < 16; i++){
			int key = list[i];

			if(key != 0){
				track.add(makeEvent(144, 9, key, 100, i));
				track.add(makeEvent(128, 9, key, 100, i + 1));
			}
		}
	}

	// This method just facilitates the creation of MidiEvents
	public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
		MidiEvent event = null;
		try{
			ShortMessage a = new ShortMessage();
			a.setMessage(comd, chan, one, two);
			event = new MidiEvent(a, tick);
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return event;
	}
}
