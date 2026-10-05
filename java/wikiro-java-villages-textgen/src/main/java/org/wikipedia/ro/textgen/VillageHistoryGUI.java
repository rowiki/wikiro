
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
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class VillageHistoryGUI extends JFrame {

    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font FIELD_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font OUTPUT_FONT = new Font("Monospaced", Font.PLAIN, 14);

    private final JTextField bauerNameField = createField(28);
    private final JTextField bauerDescriptionField = createField(28);
    private final JTextField bauerPageField = createField(14);
    private final JTextField spechtNameField = createField(28);
    private final JTextField weissNameField = createField(28);

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

    private final JTextField mdgrNameField = createField(28);
    private final JTextField mdgrCommuneField = createField(28);
    private final JTextField mdgrPlasaField = createField(28);
    private final JTextField mdgrCountyField = createField(28);
    private final JTextField mdgrPopField = createField(14);
    private final JTextField mdgrVolumeField = createField(8);
    private final JTextField mdgrPageField = createField(8);
    private final JTextField mdgrRefTitleField = createField(28);
    private final JCheckBox mdgrSameCommuneCheck = new JCheckBox("Aceeași comună");
    private final JCheckBox mdgrIsResedintaCheck = new JCheckBox("Reședință de comună");
    private final JCheckBox mdgrCommuneSameNameCheck = new JCheckBox("Cu același nume");
    private final JCheckBox mdgrSamePlasaCheck = new JCheckBox("Aceeași plasă");
    private final JCheckBox mdgrSameCountyCheck = new JCheckBox("Același județ");
    private final JTextField mdgrExtraDescriptionField = createField(48);

    private final JTextField vilComComunaField = createField(28);
    private final JTextField vilComPlasaField = createField(28);
    private final JTextField vilComCountyField = createField(28);
    private final JTextField vilComPageField = createField(14);
    private final JTextField vilComCaseField = createField(8);
    private final JTextField vilComFamiliiField = createField(8);
    private final JTextField vilComBisericiField = createField(8);

    private final JTextField orgComComuneField = createField(48);
    private final JTextField orgComPlasaField = createField(28);
    private final JTextField orgComCountyField = createField(28);
    private final JTextField orgComPageField = createField(14);
    private final JTextField orgComCaseField = createField(8);
    private final JTextField orgComFamiliiField = createField(8);
    private final JTextField orgComBisericiField = createField(8);

    private final JTextField legeComunalaPlasaField = createField(28);
    private final JTextField legeComunalaCountyField = createField(28);
    private final JTextField legeComunalaPageField = createField(14);
    private final JCheckBox legeComunalaSingleVillageCheck = new JCheckBox("Doar satul eponim");
    private final JTextField legeComunalaVillagesField = createField(28);
    private final JTextArea legeComunalaExtArea = new JTextArea(3, 28);
    private final JTextField legeComunalaCaseField = createField(8);
    private final JTextField legeComunalaFamiliiField = createField(8);
    private final JTextField legeComunalaBisericiField = createField(8);

    private final JCheckBox satLegeComunalaCapitalCheck = new JCheckBox("A devenit reședință de comună");
    private final JCheckBox satLegeComunalaSameNameCheck = new JCheckBox("Comună cu același nume");
    private final JTextField satLegeComunalaCommuneField = createField(28);
    private final JTextField satLegeComunalaPlasaField = createField(28);
    private final JTextField satLegeComunalaCountyField = createField(28);
    private final JTextField satLegeComunalaPageField = createField(14);

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

        JPanel sourcesPanel = new JPanel();
        sourcesPanel.setLayout(new BoxLayout(sourcesPanel, BoxLayout.Y_AXIS));
        sourcesPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        sourcesPanel.add(createSection("Bauer's Memoirs", new String[] { "Name", "Description", "Page" },
            new JTextField[] { bauerNameField, bauerDescriptionField, bauerPageField }));

        sourcesPanel.add(createSection("Specht Map", new String[] { "Name" }, new JTextField[] { spechtNameField }));

        sourcesPanel.add(createSection("Weiss Map", new String[] { "Name" }, new JTextField[] { weissNameField }));

        sourcesPanel.add(createSection("Catagraphy 1831",
            new String[] { "Name", "Plasa", "County", "Moșie", "Owner", "Families", "Feciori", "Page" },
            new JTextField[] { cataNameField, cataPlasaField, cataCountyField, cataMosieField, cataOwnerField, cataPopField,
                cataFeciField, cataPageField }));

        sourcesPanel.add(createSatLegeComunalaSection());

        sourcesPanel.add(createMdgrSection());

        JScrollPane sourcesScroll = new JScrollPane(sourcesPanel);
        sourcesScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JPanel indexesPanel = new JPanel();
        indexesPanel.setLayout(new BoxLayout(indexesPanel, BoxLayout.Y_AXIS));
        indexesPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        indexesPanel.add(createSection("Index 1954", new String[] { "Name", "Description", "Page" },
            new JTextField[] { idx1954NameField, idx1954DescrField, idx1954PageField }));

        indexesPanel.add(createSection("Index 1956", new String[] { "Name", "Description", "Page" },
            new JTextField[] { idx1956NameField, idx1956DescrField, idx1956PageField }));

        JScrollPane indexesScroll = new JScrollPane(indexesPanel);
        indexesScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JPanel reforma1864Panel = new JPanel();
        reforma1864Panel.setLayout(new BoxLayout(reforma1864Panel, BoxLayout.Y_AXIS));
        reforma1864Panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        reforma1864Panel.add(createLegeComunalaSection());

        reforma1864Panel.add(createSection("Sate din comună",
            new String[] { "Comună", "Plasă", "Județ", "Pagină", "Case", "Familii", "Biserici" },
            new JTextField[] { vilComComunaField, vilComPlasaField, vilComCountyField, vilComPageField, vilComCaseField,
                vilComFamiliiField, vilComBisericiField }));

        reforma1864Panel.add(createSection("Comune organizate",
            new String[] { "Comune", "Plasă", "Județ", "Pagină", "Case", "Familii", "Biserici" },
            new JTextField[] { orgComComuneField, orgComPlasaField, orgComCountyField, orgComPageField, orgComCaseField,
                orgComFamiliiField, orgComBisericiField }));

        JScrollPane reforma1864Scroll = new JScrollPane(reforma1864Panel);
        reforma1864Scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(LABEL_FONT);
        tabbedPane.addTab("Surse istorice", sourcesScroll);
        tabbedPane.addTab("Indici", indexesScroll);
        tabbedPane.addTab("Reformă 1864", reforma1864Scroll);

        add(tabbedPane, BorderLayout.CENTER);

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
        params.setWeissName(getTextOrNull(weissNameField));
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
        params.setMdgrName(getTextOrNull(mdgrNameField));
        params.setMdgrCommune(getTextOrNull(mdgrCommuneField));
        params.setMdgrPlasa(getTextOrNull(mdgrPlasaField));
        params.setMdgrCounty(getTextOrNull(mdgrCountyField));
        params.setMdgrPop(getTextOrNull(mdgrPopField));
        params.setMdgrVolume(getTextOrNull(mdgrVolumeField));
        params.setMdgrPage(getTextOrNull(mdgrPageField));
        params.setMdgrRefTitle(getTextOrNull(mdgrRefTitleField));
        params.setMdgrExtraDescription(getTextOrNull(mdgrExtraDescriptionField));
        params.setMdgrSameCommune(mdgrSameCommuneCheck.isSelected());
        params.setMdgrIsResedinta(mdgrIsResedintaCheck.isSelected());
        params.setMdgrCommuneSameName(mdgrCommuneSameNameCheck.isSelected());
        params.setMdgrSamePlasa(mdgrSamePlasaCheck.isSelected());
        params.setMdgrSameCounty(mdgrSameCountyCheck.isSelected());
        params.setVilComComuna(getTextOrNull(vilComComunaField));
        params.setVilComPlasa(getTextOrNull(vilComPlasaField));
        params.setVilComCounty(getTextOrNull(vilComCountyField));
        params.setVilComPage(getTextOrNull(vilComPageField));
        params.setVilComCase(getTextOrNull(vilComCaseField));
        params.setVilComFamilii(getTextOrNull(vilComFamiliiField));
        params.setVilComBiserici(getTextOrNull(vilComBisericiField));
        params.setOrgComComune(getTextOrNull(orgComComuneField));
        params.setOrgComPlasa(getTextOrNull(orgComPlasaField));
        params.setOrgComCounty(getTextOrNull(orgComCountyField));
        params.setOrgComPage(getTextOrNull(orgComPageField));
        params.setOrgComCase(getTextOrNull(orgComCaseField));
        params.setOrgComFamilii(getTextOrNull(orgComFamiliiField));
        params.setOrgComBiserici(getTextOrNull(orgComBisericiField));
        params.setLegeComunalaPlasa(getTextOrNull(legeComunalaPlasaField));
        params.setLegeComunalaCounty(getTextOrNull(legeComunalaCountyField));
        params.setLegeComunalaPage(getTextOrNull(legeComunalaPageField));
        params.setLegeComunalaSingleVillage(legeComunalaSingleVillageCheck.isSelected());
        params.setLegeComunalaVillages(getTextOrNull(legeComunalaVillagesField));
        params.setLegeComunalaExt(getTextOrNull(legeComunalaExtArea));
        params.setLegeComunalaCase(getTextOrNull(legeComunalaCaseField));
        params.setLegeComunalaFamilii(getTextOrNull(legeComunalaFamiliiField));
        params.setLegeComunalaBiserici(getTextOrNull(legeComunalaBisericiField));
        params.setSatLegeComunalaCapital(satLegeComunalaCapitalCheck.isSelected());
        params.setSatLegeComunalaSameName(satLegeComunalaSameNameCheck.isSelected());
        params.setSatLegeComunalaCommune(getTextOrNull(satLegeComunalaCommuneField));
        params.setSatLegeComunalaPlasa(getTextOrNull(satLegeComunalaPlasaField));
        params.setSatLegeComunalaCounty(getTextOrNull(satLegeComunalaCountyField));
        params.setSatLegeComunalaPage(getTextOrNull(satLegeComunalaPageField));

        VillageHistoryService service = new VillageHistoryService();
        String result = service.buildText(params);
        outputArea.setText(result);
        outputArea.selectAll();
    }

    private void clearForm() {
        for (JTextField field : new JTextField[] { bauerNameField, bauerDescriptionField, bauerPageField, spechtNameField,
            weissNameField, cataNameField, cataPlasaField, cataCountyField, cataMosieField, cataOwnerField, cataPopField, cataFeciField,
            cataPageField, idx1954NameField, idx1954DescrField, idx1954PageField, idx1956NameField, idx1956DescrField,
            idx1956PageField, mdgrNameField, mdgrCommuneField, mdgrPlasaField, mdgrCountyField, mdgrPopField,
            mdgrVolumeField, mdgrPageField, mdgrRefTitleField, mdgrExtraDescriptionField,
            vilComComunaField, vilComPlasaField, vilComCountyField, vilComPageField, vilComCaseField, vilComFamiliiField, vilComBisericiField,
            orgComComuneField, orgComPlasaField, orgComCountyField, orgComPageField, orgComCaseField, orgComFamiliiField, orgComBisericiField,
            legeComunalaPlasaField, legeComunalaCountyField, legeComunalaPageField, legeComunalaVillagesField,
            legeComunalaCaseField, legeComunalaFamiliiField, legeComunalaBisericiField,
            satLegeComunalaCommuneField, satLegeComunalaPlasaField, satLegeComunalaCountyField, satLegeComunalaPageField }) {
            field.setText("");
        }
        for (JCheckBox box : new JCheckBox[] { mdgrSameCommuneCheck, mdgrIsResedintaCheck, mdgrCommuneSameNameCheck,
            mdgrSamePlasaCheck, mdgrSameCountyCheck, legeComunalaSingleVillageCheck,
            satLegeComunalaCapitalCheck, satLegeComunalaSameNameCheck }) {
            box.setSelected(false);
        }
        updateMdgrCommuneFieldStates();
        legeComunalaVillagesField.setEnabled(true);
        legeComunalaExtArea.setText("");
        satLegeComunalaCommuneField.setEnabled(true);
        outputArea.setText("");
    }

    private JPanel createMdgrSection() {
        JPanel section = new JPanel(new GridBagLayout());
        section.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
            "Marele Dicționar Geografic al României", 0, 0, LABEL_FONT));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel nameLabel = new JLabel("Name:"); nameLabel.setFont(LABEL_FONT); section.add(nameLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1; gbc.gridwidth = 3;
        mdgrNameField.setFont(FIELD_FONT); section.add(mdgrNameField, gbc);
        gbc.gridwidth = 1;

        // Row 1: Commune field + checkboxes
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel communeLabel = new JLabel("Comună:"); communeLabel.setFont(LABEL_FONT); section.add(communeLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        mdgrCommuneField.setFont(FIELD_FONT); section.add(mdgrCommuneField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        mdgrSameCommuneCheck.setFont(LABEL_FONT); section.add(mdgrSameCommuneCheck, gbc);
        gbc.gridx = 3;
        mdgrIsResedintaCheck.setFont(LABEL_FONT); section.add(mdgrIsResedintaCheck, gbc);

        // Row 2: communeSameName checkbox (only relevant when isResedinta)
        gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth = 2;
        mdgrCommuneSameNameCheck.setFont(LABEL_FONT);
        mdgrCommuneSameNameCheck.setEnabled(false);
        section.add(mdgrCommuneSameNameCheck, gbc);
        gbc.gridwidth = 1;

        // Row 3: Plasa
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel plasaLabel = new JLabel("Plasă:"); plasaLabel.setFont(LABEL_FONT); section.add(plasaLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        mdgrPlasaField.setFont(FIELD_FONT); section.add(mdgrPlasaField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        mdgrSamePlasaCheck.setFont(LABEL_FONT); section.add(mdgrSamePlasaCheck, gbc);

        // Row 4: County
        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel countyLabel = new JLabel("Județ:"); countyLabel.setFont(LABEL_FONT); section.add(countyLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        mdgrCountyField.setFont(FIELD_FONT); section.add(mdgrCountyField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        mdgrSameCountyCheck.setFont(LABEL_FONT); section.add(mdgrSameCountyCheck, gbc);

        // Row 5: Population
        gbc.gridx = 0; gbc.gridy = 5; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel popLabel = new JLabel("Populație:"); popLabel.setFont(LABEL_FONT); section.add(popLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        mdgrPopField.setFont(FIELD_FONT); section.add(mdgrPopField, gbc);

        // Row 6: Volume, Page, Ref title
        gbc.gridx = 0; gbc.gridy = 6; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel volLabel = new JLabel("Volum:"); volLabel.setFont(LABEL_FONT); section.add(volLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0;
        mdgrVolumeField.setFont(FIELD_FONT); section.add(mdgrVolumeField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel pageLabel = new JLabel("Pagină:"); pageLabel.setFont(LABEL_FONT); section.add(pageLabel, gbc);
        gbc.gridx = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0;
        mdgrPageField.setFont(FIELD_FONT); section.add(mdgrPageField, gbc);

        // Row 7: Ref title
        gbc.gridx = 0; gbc.gridy = 7; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel refTitleLabel = new JLabel("Titlu ref:"); refTitleLabel.setFont(LABEL_FONT); section.add(refTitleLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1; gbc.gridwidth = 3;
        mdgrRefTitleField.setFont(FIELD_FONT); section.add(mdgrRefTitleField, gbc);
        gbc.gridwidth = 1;

        // Row 8: Extra description
        gbc.gridx = 0; gbc.gridy = 8; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel extraDescrLabel = new JLabel("Descriere extra:"); extraDescrLabel.setFont(LABEL_FONT); section.add(extraDescrLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1; gbc.gridwidth = 3;
        mdgrExtraDescriptionField.setFont(FIELD_FONT); section.add(mdgrExtraDescriptionField, gbc);
        gbc.gridwidth = 1;

        // Interaction logic
        mdgrSameCommuneCheck.addActionListener(e -> updateMdgrCommuneFieldStates());
        mdgrIsResedintaCheck.addActionListener(e -> updateMdgrCommuneFieldStates());
        mdgrCommuneSameNameCheck.addActionListener(e -> updateMdgrCommuneFieldStates());
        mdgrSamePlasaCheck.addActionListener(e -> mdgrPlasaField.setEnabled(!mdgrSamePlasaCheck.isSelected()));
        mdgrSameCountyCheck.addActionListener(e -> mdgrCountyField.setEnabled(!mdgrSameCountyCheck.isSelected()));

        return section;
    }

    private void updateMdgrCommuneFieldStates() {
        boolean isResedinta = mdgrIsResedintaCheck.isSelected();
        boolean sameCommune = mdgrSameCommuneCheck.isSelected();
        boolean communeSameName = mdgrCommuneSameNameCheck.isSelected();
        mdgrSameCommuneCheck.setEnabled(!isResedinta);
        mdgrCommuneSameNameCheck.setEnabled(isResedinta);
        mdgrCommuneField.setEnabled(!sameCommune && !(isResedinta && communeSameName));
    }

    private JPanel createLegeComunalaSection() {
        JPanel section = new JPanel(new GridBagLayout());
        section.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
            "Legea comunală din 1864", 0, 0, LABEL_FONT));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel plasaLabel = new JLabel("Plasă:"); plasaLabel.setFont(LABEL_FONT); section.add(plasaLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        section.add(legeComunalaPlasaField, gbc);

        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel countyLabel = new JLabel("Județ:"); countyLabel.setFont(LABEL_FONT); section.add(countyLabel, gbc);
        gbc.gridx = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        section.add(legeComunalaCountyField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel pageLabel = new JLabel("Pagină:"); pageLabel.setFont(LABEL_FONT); section.add(pageLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        section.add(legeComunalaPageField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel caseLabel = new JLabel("Case:"); caseLabel.setFont(LABEL_FONT); section.add(caseLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        section.add(legeComunalaCaseField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel familiiLabel = new JLabel("Familii:"); familiiLabel.setFont(LABEL_FONT); section.add(familiiLabel, gbc);
        gbc.gridx = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        section.add(legeComunalaFamiliiField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel bisericiLabel = new JLabel("Biserici:"); bisericiLabel.setFont(LABEL_FONT); section.add(bisericiLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        section.add(legeComunalaBisericiField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        legeComunalaSingleVillageCheck.setFont(LABEL_FONT);
        section.add(legeComunalaSingleVillageCheck, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 5; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel villagesLabel = new JLabel("Sate:"); villagesLabel.setFont(LABEL_FONT); section.add(villagesLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1; gbc.gridwidth = 3;
        section.add(legeComunalaVillagesField, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 6; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel extLabel = new JLabel("Așezări identificate:"); extLabel.setFont(LABEL_FONT); section.add(extLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1; gbc.weighty = 1; gbc.gridwidth = 3;
        legeComunalaExtArea.setFont(FIELD_FONT);
        legeComunalaExtArea.setLineWrap(true);
        legeComunalaExtArea.setWrapStyleWord(true);
        JScrollPane extScroll = new JScrollPane(legeComunalaExtArea);
        extScroll.setPreferredSize(new Dimension(300, 60));
        section.add(extScroll, gbc);
        gbc.gridwidth = 1; gbc.weighty = 0; gbc.anchor = GridBagConstraints.WEST;

        legeComunalaSingleVillageCheck.addActionListener(e ->
            legeComunalaVillagesField.setEnabled(!legeComunalaSingleVillageCheck.isSelected()));

        return section;
    }

    private JPanel createSatLegeComunalaSection() {
        JPanel section = new JPanel(new GridBagLayout());
        section.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
            "Satul și legea comunală din 1864", 0, 0, LABEL_FONT));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        satLegeComunalaCapitalCheck.setFont(LABEL_FONT);
        section.add(satLegeComunalaCapitalCheck, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel communeLabel = new JLabel("Comună:"); communeLabel.setFont(LABEL_FONT); section.add(communeLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        section.add(satLegeComunalaCommuneField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        satLegeComunalaSameNameCheck.setFont(LABEL_FONT);
        section.add(satLegeComunalaSameNameCheck, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel plasaLabel = new JLabel("Plasă:"); plasaLabel.setFont(LABEL_FONT); section.add(plasaLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        section.add(satLegeComunalaPlasaField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel countyLabel = new JLabel("Județ:"); countyLabel.setFont(LABEL_FONT); section.add(countyLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        section.add(satLegeComunalaCountyField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel pageLabel = new JLabel("Pagină:"); pageLabel.setFont(LABEL_FONT); section.add(pageLabel, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        section.add(satLegeComunalaPageField, gbc);

        satLegeComunalaSameNameCheck.addActionListener(e ->
            satLegeComunalaCommuneField.setEnabled(!satLegeComunalaSameNameCheck.isSelected()));

        return section;
    }

    private String getTextOrNull(JTextField field) {
        String text = field.getText().trim();
        return text.isEmpty() ? null : text;
    }

    private String getTextOrNull(JTextArea area) {
        String text = area.getText().trim();
        return text.isEmpty() ? null : text;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VillageHistoryGUI().setVisible(true));
    }
}
