package ph.edu.lendly;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.text.View;

/** Shared Swing components for the Lendly desktop screens. */
final class LendlyView {
    static final Color CANVAS = new Color(0xF5F7F4);
    static final Color INK = new Color(0x143F30);
    static final Color MUTED = new Color(0x51645B);
    static final Color GREEN = new Color(0x28654C);
    static final Color MINT = new Color(0xE7F2EA);
    static final Color LINE = new Color(0xDCE3DE);
    static final Color ERROR_COLOR = new Color(0xA62F36);
    static final Color WHITE = Color.WHITE;

    private LendlyView() {}

    static JPanel stack() {
        var panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    static void append(JPanel panel, JComponent item, int gap) {
        if (gap > 0) panel.add(Box.createVerticalStrut(gap));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(item);
    }

    static JLabel label(String text, int size, boolean bold, Color color) {
        var label = new JLabel();
        label.putClientProperty("html.disable", Boolean.TRUE);
        label.setText(text);
        label.setForeground(color);
        label.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size));
        return label;
    }

    static JTextArea note(String text, Color color) {
        return note(text, color, 420);
    }

    static JTextArea note(String text, Color color, int wrapWidth) {
        var note = new JTextArea(text) {
            @Override public Dimension getPreferredSize() {
                if (getText().isEmpty()) return new Dimension(wrapWidth, 0);
                var view = getUI().getRootView(this);
                view.setSize(wrapWidth, Short.MAX_VALUE);
                return new Dimension(wrapWidth, (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS)));
            }

            @Override public Dimension getMinimumSize() {
                return new Dimension(0, getPreferredSize().height);
            }
        };
        note.setEditable(false);
        note.setFocusable(false);
        note.setOpaque(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        note.setForeground(color);
        note.setBorder(BorderFactory.createEmptyBorder());
        return note;
    }

    static JPanel card(Color background) {
        return card(background, 0);
    }

    static JPanel card(Color background, int preferredWidth) {
        var panel = new RoundedPanel(background, 24) {
            @Override public Dimension getPreferredSize() {
                var size = super.getPreferredSize();
                if (preferredWidth > 0) size.width = preferredWidth;
                return size;
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(26, 28, 26, 28));
        return panel;
    }

    static JPanel field(String title, JComponent control) {
        var panel = stack();
        var caption = label(title, 13, true, INK);
        caption.setLabelFor(control);
        control.getAccessibleContext().setAccessibleName(title);
        append(panel, caption, 0);
        append(panel, control, 7);
        return panel;
    }

    static <T extends JTextField> T input(T field) {
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        field.setForeground(INK);
        field.setCaretColor(INK);
        field.setOpaque(false);
        field.setBorder(new InputBorder());
        field.setMinimumSize(new Dimension(90, 43));
        field.setPreferredSize(new Dimension(240, 43));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent event) { field.repaint(); }
            @Override public void focusLost(FocusEvent event) { field.repaint(); }
        });
        return field;
    }

    static JTextField input(String value) {
        return input(new JTextField(value));
    }

    static JButton button(String text, boolean primary, Runnable action) {
        return button(text, primary, false, action);
    }

    static JButton button(String text, boolean primary, boolean outline, Runnable action) {
        var button = new JButton(text) {
            @Override protected void paintComponent(Graphics graphics) {
                var g = smooth(graphics);
                Color fill = primary ? GREEN : outline ? WHITE : MINT;
                if (!isEnabled()) fill = LINE;
                else if (getModel().isPressed()) fill = fill.darker();
                else if (getModel().isRollover()) fill = primary ? new Color(0x2C7258) : new Color(0xD9EADF);
                g.setColor(fill);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                if (outline) {
                    g.setColor(LINE);
                    g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                }
                if (isFocusOwner()) {
                    g.setColor(primary ? WHITE : GREEN);
                    g.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, 11, 11);
                }
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        button.setForeground(primary ? WHITE : GREEN);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(11, 16, 11, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setRolloverEnabled(true);
        button.addActionListener(event -> action.run());
        return button;
    }

    static JPanel row(int gap, Component... items) {
        var panel = new JPanel(new FlowLayout(FlowLayout.LEADING, gap, 0));
        panel.setOpaque(false);
        for (var item : items) panel.add(item);
        return panel;
    }

    static JPanel pair(JComponent left, JComponent right) {
        var panel = new JPanel(new GridLayout(1, 2, 14, 0));
        panel.setOpaque(false);
        panel.add(left);
        panel.add(right);
        return panel;
    }

    static JPanel top(JComponent body) {
        var panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(body, BorderLayout.NORTH);
        return panel;
    }

    static JScrollPane scroll(JComponent body) {
        var scroll = new JScrollPane(new ScrollContent(body));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    static JLabel brand() {
        return label("lendly", 24, true, GREEN);
    }

    static JPanel badge(String text, Color background, Color foreground) {
        var badge = new RoundedPanel(background, 12);
        badge.setLayout(new BorderLayout());
        badge.setBorder(new EmptyBorder(5, 8, 5, 8));
        badge.add(label(text, 13, true, foreground));
        badge.setMaximumSize(badge.getPreferredSize());
        return badge;
    }

    private static Graphics2D smooth(Graphics graphics) {
        var g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g;
    }

    private static final class InputBorder extends AbstractBorder {
        @Override public Insets getBorderInsets(Component component, Insets insets) {
            insets.set(8, 13, 8, 13);
            return insets;
        }

        @Override public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
            var g = smooth(graphics);
            g.setColor(component.isFocusOwner() ? GREEN : LINE);
            g.drawRoundRect(x, y, width - 1, height - 1, 12, 12);
            g.dispose();
        }
    }

    private static final class ScrollContent extends JPanel implements Scrollable {
        private ScrollContent(JComponent content) {
            super(new BorderLayout());
            setOpaque(false);
            add(content);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(16, visible.height - 16);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport viewport && getPreferredSize().height <= viewport.getHeight();
        }
    }

    private static class RoundedPanel extends JPanel {
        private final Color background;
        private final int radius;

        private RoundedPanel(Color background, int radius) {
            this.background = background;
            this.radius = radius;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics graphics) {
            var g = smooth(graphics);
            g.setColor(background);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
