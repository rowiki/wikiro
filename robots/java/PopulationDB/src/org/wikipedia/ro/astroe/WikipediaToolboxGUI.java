package org.wikipedia.ro.astroe;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.reflections.Reflections;
import org.wikipedia.Wiki;
import org.wikipedia.ro.astroe.generators.Generator;
import org.wikipedia.ro.astroe.generators.PageGenerator;
import org.wikipedia.ro.astroe.operations.Operation;

public class WikipediaToolboxGUI {

    private static ResourceBundle bundle;
    private static Wiki sourceWiki = null;
    private static Wiki targetWiki = null;
    private static Map<String, Component> dataComponentsMap = new HashMap<String, Component>();

    public static void main(String[] args) {
        bundle = ResourceBundle.getBundle("uitexts.uitexts", new Locale("ro"));
        // TODO Auto-generated method stub
        JFrame frame = new JFrame();

        frame.setTitle(bundle.getString("frame.title"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Dimension paneSize = new Dimension(650, 500);

        JPanel panel = new JPanel();
        frame.setContentPane(panel);
        BorderLayout mainLayout = new BorderLayout();
        mainLayout.setVgap(40);
        panel.setLayout(mainLayout);

        BorderLayout wikiConfigLayout = new BorderLayout();
        JPanel wikiConfigPanel = new JPanel(wikiConfigLayout);
        wikiConfigLayout.setHgap(10);
        JPanel sourceTargetConfigPanel = createSourceAndTargetWikiConfigPanel();
        wikiConfigPanel.add(sourceTargetConfigPanel, BorderLayout.WEST);
        JPanel loginConfigPanel = createLoginConfigPanel();
        wikiConfigPanel.add(loginConfigPanel, BorderLayout.EAST);
        panel.add(wikiConfigPanel, BorderLayout.NORTH);

        JPanel generatorConfigPanel = createGeneratorConfigPanel();
        panel.add(generatorConfigPanel, BorderLayout.CENTER);
        
        JPanel actionConfigPanel = createActionConfigPanel();
        panel.add(actionConfigPanel, BorderLayout.SOUTH);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((int) (screenSize.getWidth() - 400) / 2, (int) (screenSize.getHeight() - 400) / 2);
        frame.pack();
        frame.setVisible(true);
    }

    private static JPanel createActionConfigPanel() {
        BorderLayout actionConfigLayout = new BorderLayout();
        actionConfigLayout.setVgap(4);
        JPanel actionConfigPanel = new JPanel(actionConfigLayout);
        
        JPanel actionChoicePanel = new JPanel();
        GroupLayout actionChoiceLayout = new GroupLayout(actionChoicePanel);
        actionChoiceLayout.setAutoCreateGaps(true);
        actionChoicePanel.setLayout(actionChoiceLayout);
        JLabel actionLabel = new JLabel(bundle.getString("generator"));
        JComboBox<ChoiceElement> actionDropDown = new JComboBox<ChoiceElement>();
        SequentialGroup vGroup = actionChoiceLayout.createSequentialGroup();
        vGroup.addGroup(actionChoiceLayout.createParallelGroup(Alignment.BASELINE).addComponent(actionLabel)
            .addComponent(actionDropDown));
        actionChoiceLayout.setVerticalGroup(vGroup);
        SequentialGroup hGroup = actionChoiceLayout.createSequentialGroup();
        hGroup.addGroup(actionChoiceLayout.createParallelGroup().addComponent(actionLabel));
        hGroup.addGroup(actionChoiceLayout.createParallelGroup().addComponent(actionDropDown));
        actionChoiceLayout.setHorizontalGroup(hGroup);
        
        Reflections refl = new Reflections("org.wikipedia.ro.astroe.operations");
        Set<Class<?>> operationClasses = refl.getTypesAnnotatedWith(Operation.class);
        for (Class<?> eachOperationClass : operationClasses) {
            Operation annotation = eachOperationClass.getAnnotation(Operation.class);
            ChoiceElement el = new ChoiceElement();
            el.clazz = (Class<Generator>) eachOperationClass;
            el.label = bundle.getString(annotation.labelKey());
            actionDropDown.addItem(el);
        }
        actionConfigPanel.add(actionChoicePanel, BorderLayout.NORTH);
        
        JPanel configurationPanel = new JPanel();
        JButton goButton = new JButton(bundle.getString("go"));
        configurationPanel.add(goButton);
        actionConfigPanel.add(configurationPanel, BorderLayout.SOUTH);
        return actionConfigPanel;
    }

    private static JPanel createGeneratorConfigPanel() {
        BorderLayout generatorConfigLayout = new BorderLayout();
        generatorConfigLayout.setVgap(4);
        JPanel generatorConfigPanel = new JPanel(generatorConfigLayout);

        JPanel generatorChoicePanel = new JPanel();
        GroupLayout generatorChoiceLayout = new GroupLayout(generatorChoicePanel);
        generatorChoiceLayout.setAutoCreateGaps(true);
        generatorChoicePanel.setLayout(generatorChoiceLayout);
        JLabel generatorLabel = new JLabel(bundle.getString("generator"));
        JComboBox<ChoiceElement> generatorDropDown = new JComboBox<ChoiceElement>();
        SequentialGroup vGroup = generatorChoiceLayout.createSequentialGroup();
        vGroup.addGroup(generatorChoiceLayout.createParallelGroup(Alignment.BASELINE).addComponent(generatorLabel)
            .addComponent(generatorDropDown));
        generatorChoiceLayout.setVerticalGroup(vGroup);
        SequentialGroup hGroup = generatorChoiceLayout.createSequentialGroup();
        hGroup.addGroup(generatorChoiceLayout.createParallelGroup().addComponent(generatorLabel));
        hGroup.addGroup(generatorChoiceLayout.createParallelGroup().addComponent(generatorDropDown));
        generatorChoiceLayout.setHorizontalGroup(hGroup);

        final JPanel generatorOptionsPanel = new JPanel();
        generatorConfigPanel.add(generatorOptionsPanel, BorderLayout.SOUTH);
        generatorDropDown.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    ChoiceElement item = (ChoiceElement) e.getItem();
                    PageGenerator annotation = item.clazz.getAnnotation(PageGenerator.class);
                    int noOfConfigs = annotation.stringsConfigNumber();
                    String[] configKeys = annotation.stringsConfigLabelKeys();
                    generatorOptionsPanel.removeAll();
                    GroupLayout generatorOptionsLayout = new GroupLayout(generatorOptionsPanel);
                    generatorOptionsLayout.setAutoCreateGaps(true);
                    generatorOptionsPanel.setLayout(generatorOptionsLayout);
                    SequentialGroup vGroup = generatorOptionsLayout.createSequentialGroup();
                    SequentialGroup hGroup = generatorOptionsLayout.createSequentialGroup();
                    ParallelGroup[] pvGroups =
                        new ParallelGroup[] { generatorOptionsLayout.createParallelGroup(),
                            generatorOptionsLayout.createParallelGroup() };
                    for (int i = 0; i < noOfConfigs; i++) {
                        JLabel crtLabel = new JLabel(bundle.getString(configKeys[i]));
                        JTextField crtTF = new JTextField();
                        crtTF.setPreferredSize(new Dimension(200, 20));
                        dataComponentsMap.put(configKeys[i], crtTF);
                        pvGroups[0].addComponent(crtLabel);
                        pvGroups[1].addComponent(crtTF);
                        vGroup.addGroup(
                            generatorOptionsLayout.createParallelGroup(Alignment.BASELINE).addComponent(crtLabel).addComponent(crtTF));
                    }
                    hGroup.addGroup(pvGroups[0]).addGroup(pvGroups[1]);
                    generatorOptionsLayout.setHorizontalGroup(hGroup);
                    generatorOptionsLayout.setVerticalGroup(vGroup);
                }
            }
        });

        Reflections refl = new Reflections("org.wikipedia.ro.astroe.generators");
        Set<Class<?>> generatorClasses = refl.getTypesAnnotatedWith(PageGenerator.class);
        for (Class<?> eachGeneratorClass : generatorClasses) {
            PageGenerator annotation = eachGeneratorClass.getAnnotation(PageGenerator.class);
            ChoiceElement el = new ChoiceElement();
            el.clazz = (Class<Generator>) eachGeneratorClass;
            el.label = bundle.getString(annotation.labelKey());
            generatorDropDown.addItem(el);
        }
        generatorConfigPanel.add(generatorChoicePanel, BorderLayout.NORTH);
        return generatorConfigPanel;
    }

    private static JPanel createSourceAndTargetWikiConfigPanel() {
        JPanel sourceTargetConfigPanel = new JPanel();
        GroupLayout sourceTargetConfigLayout = new GroupLayout(sourceTargetConfigPanel);
        JLabel sourceWikiLabel = new JLabel(bundle.getString("sourcewiki.label"));
        JLabel targetWikiLabel = new JLabel(bundle.getString("targetwiki.label"));
        final JTextField sourceWikiTextField = new JTextField();
        sourceWikiTextField.setPreferredSize(new Dimension(50, 20));
        final JTextField targetWikiTextField = new JTextField();
        targetWikiTextField.setPreferredSize(new Dimension(50, 20));
        sourceTargetConfigPanel.setLayout(sourceTargetConfigLayout);
        sourceTargetConfigLayout.setAutoCreateGaps(true);
        SequentialGroup hGroup = sourceTargetConfigLayout.createSequentialGroup();
        hGroup.addGroup(
            sourceTargetConfigLayout.createParallelGroup().addComponent(sourceWikiLabel).addComponent(targetWikiLabel));
        hGroup.addGroup(sourceTargetConfigLayout.createParallelGroup().addComponent(sourceWikiTextField)
            .addComponent(targetWikiTextField));
        sourceTargetConfigLayout.setHorizontalGroup(hGroup);
        SequentialGroup vGroup = sourceTargetConfigLayout.createSequentialGroup();
        vGroup.addGroup(sourceTargetConfigLayout.createParallelGroup(Alignment.BASELINE).addComponent(sourceWikiLabel)
            .addComponent(sourceWikiTextField));
        vGroup.addGroup(sourceTargetConfigLayout.createParallelGroup(Alignment.BASELINE).addComponent(targetWikiLabel)
            .addComponent(targetWikiTextField));
        sourceTargetConfigLayout.setVerticalGroup(vGroup);
        return sourceTargetConfigPanel;
    }

    private static JPanel createLoginConfigPanel() {
        JPanel loginConfigPanel = new JPanel();
        GroupLayout loginConfigLayout = new GroupLayout(loginConfigPanel);
        JLabel usernameLabel = new JLabel(bundle.getString("username"));
        JLabel passwordLabel = new JLabel(bundle.getString("password"));
        JTextField usernameTextField = new JTextField();
        usernameTextField.setPreferredSize(new Dimension(150, 20));
        JTextField passwordTextField = new JPasswordField();
        passwordTextField.setPreferredSize(new Dimension(150, 20));
        JButton loginButton = new JButton(bundle.getString("login.button"));
        loginConfigPanel.setLayout(loginConfigLayout);
        loginConfigLayout.setAutoCreateGaps(true);
        SequentialGroup loginHGroup = loginConfigLayout.createSequentialGroup();
        loginHGroup
            .addGroup(loginConfigLayout.createParallelGroup().addComponent(usernameLabel).addComponent(passwordLabel));
        loginHGroup.addGroup(loginConfigLayout.createParallelGroup().addComponent(usernameTextField)
            .addComponent(passwordTextField).addComponent(loginButton));
        loginConfigLayout.setHorizontalGroup(loginHGroup);
        SequentialGroup loginVGroup = loginConfigLayout.createSequentialGroup();
        loginVGroup.addGroup(loginConfigLayout.createParallelGroup(Alignment.BASELINE).addComponent(usernameLabel)
            .addComponent(usernameTextField));
        loginVGroup.addGroup(loginConfigLayout.createParallelGroup(Alignment.BASELINE).addComponent(passwordLabel)
            .addComponent(passwordTextField));
        loginVGroup.addGroup(loginConfigLayout.createParallelGroup().addComponent(loginButton));
        loginConfigLayout.setVerticalGroup(loginVGroup);
        return loginConfigPanel;
    }

    private static class ChoiceElement {
        public String label;
        public Class<Generator> clazz;

        @Override
        public String toString() {
            return label;
        }

    }
}
