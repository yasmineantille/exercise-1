package ch.unisg.ics.interactions.teaching.was.fs2022.assignments._1.environment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class BuildingEnvironmentGUI extends JFrame {
  private final JComboBox<String> illuminanceBox;
  private final JComboBox<String> weatherBox;
  private final String[] illuminanceOptions = {"high", "low"};
  private final String[] weatherOptions = {"sunny", "cloudy"};
  private final BuildingEnvironmentAgent myAgent;
  private String illuminance;
  private String weather;

  BuildingEnvironmentGUI(BuildingEnvironmentAgent agent) {

    myAgent = agent;
    illuminance = myAgent.getWeather();
    weather = myAgent.getIlluminance();

    JPanel p = new JPanel();
    p.setLayout(new GridLayout(2, 2));

    p.add(new JLabel("Perceived illuminance:"));
    illuminanceBox = new JComboBox<>(illuminanceOptions);
    p.add(illuminanceBox);

    p.add(new JLabel("Perceived weather:"));
    weatherBox = new JComboBox<>(weatherOptions);
    p.add(weatherBox);

    getContentPane().add(p, BorderLayout.CENTER);

    JButton addButton = new JButton("Done");

    addButton.addActionListener(ev -> {
      this.illuminance = illuminanceBox.getItemAt(illuminanceBox.getSelectedIndex());
      myAgent.setIlluminance(illuminance);
      this.weather = weatherBox.getItemAt(weatherBox.getSelectedIndex());
      myAgent.setWeather(weather);
      this.dispose();
    });

    p = new JPanel();
    p.add(addButton);
    getContentPane().add(p, BorderLayout.SOUTH);

    // Make the agent terminate when the user closes
    // the GUI using the button on the upper right corner
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        myAgent.doDelete();
      }
    });

    setResizable(false);
  }

  public void showGui() {
    pack();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int centerX = (int) screenSize.getWidth() / 2;
    int centerY = (int) screenSize.getHeight() / 2;
    setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
    super.setVisible(true);
  }
}
