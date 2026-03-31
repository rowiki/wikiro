
package org.wikipedia.ro.textgen;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class VillageHistoryGUI extends JFrame {

    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font FIELD_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font OUTPUT_FONT = new Font("Monospaced", Font.PLAIN, 14);

    private final JTextField bauerNameField = createField(28);
    private final JTextField bauerDescriptionField = createField(28);
    private final JTextField bauerPageField = createField(14);
    private final JTextField spechtNameField = createField(28);

    private final JTextField cataNameField = createField(28);
    private final JTextField cataPlasaField = createField(28);
    private final JTextField cataCountyField = createField(28);
    private final JTextField cataMosieField = createField(28);
    private final JTextField cataOwnerField = createField(28);
    private final JTextField cataPopField = createField(14);
    private final JTextField cataFeciField = createField(14);
    private final JTextField cataPageField = createField(14);

    private final JTextField idx1954NameField = createField(28);
    private final JTextField idx1954DescrField = createField(28);
    private final JTextField idx1954PageField = createField(14);

    private final JTextField idx1956NameField = createField(28);
    private final JTextField idx1956DescrField = createField(28);
    private final JTextField idx1956PageField = createField(14);

    private final JTextArea outputArea = new JTextArea(8, 60);

    private static JTextField createField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(FIELD_FONT);
        return field;
    }

    public VillageHistoryGUI() {
        super("Village History Text Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        formPanel.add(createSection("Bauer's Memoirs", new String[] { "Name", "Description", "Page" },
            new JTextField[] { bauerNameField, bauerDescriptionField, bauerPageField }));

        formPanel.add(createSection("Specht Map", new String[] { "Name" }, new JTextField[] { spechtNameField }));

        formPanel.add(createSection("Catagraphy 1831",
            new String[] { "Name", "Plasa", "County", "Moșie", "Owner", "Families", "Feciori", "Page" },
            new JTextField[] { cataNameField, cataPlasaField, cataCountyField, cataMosieField, cataOwnerField, cataPopField,
                cataFeciField, cataPageField }));

        formPanel.add(createSection("Index 1954", new String[] { "Name", "Description", "Page" },
            new JTextField[] { idx1954NameField, idx1954DescrField, idx1954PageField }));

        formPanel.add(createSection("Index 1956", new String[] { "Name", "Description", "Page" },
            new JTextField[] { idx1956NameField, idx1956DescrField, idx1956PageField }));

        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(formScroll, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 6));
        JButton generateButton = new JButton("Generate");
        generateButton.setFont(BUTTON_FONT);
        generateButton.addActionListener(e -> generate());
        buttonPanel.add(generateButton);

        JButton clearButton = new JButton("Clear");
        clearButton.setFont(BUTTON_FONT);
        clearButton.addActionListener(e -> clearForm());
        buttonPanel.add(clearButton);

        outputArea.setEditable(false);
        outputArea.setFont(OUTPUT_FONT);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Generated Text"));
        outputScroll.setPreferredSize(new Dimension(0, 200));

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 4));
        bottomPanel.add(buttonPanel, BorderLayout.NORTH);
        bottomPanel.add(outputScroll, BorderLayout.CENTER);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));

        add(bottomPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(900, 1000));
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createSection(String title, String[] labels, JTextField[] fields) {
        JPanel section = new JPanel(new GridBagLayout());
        section.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title, 0, 0, LABEL_FONT));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = (i % 2) * 2;
            gbc.gridy = i / 2;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            JLabel label = new JLabel(labels[i] + ":");
            label.setFont(LABEL_FONT);
            section.add(label, gbc);

            gbc.gridx = (i % 2) * 2 + 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            section.add(fields[i], gbc);
        }

        return section;
    }

    private void generate() {
        VillageHistoryParams params = new VillageHistoryParams();
        params.setBauerName(getTextOrNull(bauerNameField));
        params.setBauerDescription(getTextOrNull(bauerDescriptionField));
        params.setBauerPage(getTextOrNull(bauerPageField));
        params.setSpechtName(getTextOrNull(spechtNameField));
        params.setCataName(getTextOrNull(cataNameField));
        params.setCataPlasa(getTextOrNull(cataPlasaField));
        params.setCataCounty(getTextOrNull(cataCountyField));
        params.setCataMosie(getTextOrNull(cataMosieField));
        params.setCataOwner(getTextOrNull(cataOwnerField));
        params.setCataPop(getTextOrNull(cataPopField));
        params.setCataFeci(getTextOrNull(cataFeciField));
        params.setCataPage(getTextOrNull(cataPageField));
        params.setIdx1954Name(getTextOrNull(idx1954NameField));
        params.setIdx1954Descr(getTextOrNull(idx1954DescrField));
        params.setIdx1954Page(getTextOrNull(idx1954PageField));
        params.setIdx1956Name(getTextOrNull(idx1956NameField));
        params.setIdx1956Descr(getTextOrNull(idx1956DescrField));
        params.setIdx1956Page(getTextOrNull(idx1956PageField));

        VillageHistoryService service = new VillageHistoryService();
        String result = service.buildText(params);
        outputArea.setText(result);
        outputArea.selectAll();
    }

    private void clearForm() {
        for (JTextField field : new JTextField[] { bauerNameField, bauerDescriptionField, bauerPageField, spechtNameField,
            cataNameField, cataPlasaField, cataCountyField, cataMosieField, cataOwnerField, cataPopField, cataFeciField,
            cataPageField, idx1954NameField, idx1954DescrField, idx1954PageField, idx1956NameField, idx1956DescrField,
            idx1956PageField }) {
            field.setText("");
        }
        outputArea.setText("");
    }

    private String getTextOrNull(JTextField field) {
        String text = field.getText().trim();
        return text.isEmpty() ? null : text;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VillageHistoryGUI().setVisible(true));
    }
}
