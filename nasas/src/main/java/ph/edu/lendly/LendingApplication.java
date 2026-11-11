package ph.edu.lendly;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

@SpringBootApplication
public class LendingApplication {
    public static void main(String[] args) {
        var builder =
                new SpringApplicationBuilder(LendingApplication.class).web(WebApplicationType.NONE);
        if (args.length == 1 && "--demo".equals(args[0])) {
            try (var context =
                    builder.run(
                            "--spring.datasource.url=jdbc:h2:mem:milestone_demo;DB_CLOSE_DELAY=-1",
                            "--spring.datasource.username=sa",
                            "--spring.datasource.password=")) {
                MilestoneDemo.run(
                        context.getBean(AccountService.class), context.getBean(LoanService.class));
            }
            return;
        }

        var context = builder.headless(false).run(args);
        var accounts = context.getBean(AccountService.class);
        var loans = context.getBean(LoanService.class);
        SwingUtilities.invokeLater(
                () -> {
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (ReflectiveOperationException
                            | javax.swing.UnsupportedLookAndFeelException ex) {
                        // Swing can use its default look and feel.
                    }
                    new MilestoneWindow(accounts, loans).setVisible(true);
                });
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
