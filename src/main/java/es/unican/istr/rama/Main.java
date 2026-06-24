package es.unican.istr.rama;

import es.unican.istr.rama.app.RamaApplication;
import es.unican.istr.rama.comparison.*;
import es.unican.istr.rama.config.*;
import es.unican.istr.rama.git.GitService;
import es.unican.istr.rama.git.github.GitHubService;
import es.unican.istr.rama.render.*;

public class Main {
    public static void main(String[] args) throws Exception {
        // The GitHub Action passes the pull request number as the first argument.
        // RAMA uses that PR number to fetch changed files and publish the final report.
        int prNumber = Integer.parseInt(args[0]);

        ConfigService configService = new ConfigService();
        ConfigurationLoadResult configuration = configService.loadConfig();
        RamaConfig config = configuration.config();

        GitService gitService = GitHubService.fromEnvironment(config);
        ComparisonService modelComparator = new EmfModelComparator(config, configService.workspacePath());

        RamaApplication application = new RamaApplication(
                config,
                gitService,
                modelComparator,
                new MunidiffRenderer(),
                new ReportCommentRenderer(new PlantUMLEncoderService()),
                configuration.warning()
        );
        application.run(prNumber);
    }
}
