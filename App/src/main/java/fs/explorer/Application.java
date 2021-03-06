package fs.explorer;

import fs.explorer.controllers.*;
import fs.explorer.controllers.FTPDialogController;
import fs.explorer.controllers.FTPInfoValidator;
import fs.explorer.models.dirtree.DirTreeModel;
import fs.explorer.providers.dirtree.local.LocalFsManager;
import fs.explorer.providers.dirtree.archives.ArchivesManager;
import fs.explorer.providers.dirtree.archives.ArchivesReader;
import fs.explorer.providers.preview.AsyncPreviewProvider;
import fs.explorer.providers.preview.DefaultPreviewProvider;
import fs.explorer.providers.preview.PreviewRenderer;
import fs.explorer.providers.preview.PreviewRenderersService;
import fs.explorer.utils.Disposable;
import fs.explorer.views.*;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Application {
    private MainWindow mainWindow;
    private boolean applicationInitialized = false;
    private String errorMessage;

    private static final String STATUS_BAR_INITIAL_MESSAGE = "Ready";

    private static final String APP_START_ERROR = "Failed to start the application";
    private static final String DISK_ACCESS_ERROR = "failed to access local disk";

    private static final long previewTaskStartDelayMilliseconds = 100;

    Application() {
        try {
            List<Disposable> disposables = new ArrayList<>();

            LocalFsManager localFsManager = new LocalFsManager();

            ArchivesReader archivesReader = new ArchivesReader();
            ArchivesManager archivesManager = new ArchivesManager(archivesReader);
            disposables.add(archivesManager);

            StatusBar statusBar = new StatusBar(STATUS_BAR_INITIAL_MESSAGE);
            StatusBarController statusBarController = new StatusBarController(statusBar);

            PreviewPane previewPane = new PreviewPane();
            List<PreviewRenderer> previewRenderers = getPreivewRenderers();
            DefaultPreviewProvider previewProvider = new DefaultPreviewProvider(
                    localFsManager, archivesManager, previewRenderers);
            AsyncPreviewProvider asyncPreviewProvider = new AsyncPreviewProvider(
                    previewProvider, previewTaskStartDelayMilliseconds);
            disposables.add(asyncPreviewProvider);
            PreviewController previewController = new PreviewController(
                    previewPane, asyncPreviewProvider, statusBarController);

            DirTreeModel dirTreeModel = new DirTreeModel();
            DirTreePane dirTreePane = new DirTreePane(dirTreeModel.getInnerTreeModel());
            DirTreeController dirTreeController = new DirTreeController(
                    dirTreePane,
                    dirTreeModel,
                    previewController,
                    statusBarController
            );
            dirTreePane.setController(dirTreeController);

            FsTypeSwitcher fsTypeSwitcher = new FsTypeSwitcher(
                    dirTreeController,
                    previewProvider,
                    localFsManager,
                    archivesManager
            );
            disposables.add(fsTypeSwitcher);

            FTPDialog ftpDialog = new FTPDialog();
            FTPInfoValidator ftpInfoValidator = new FTPInfoValidator();
            FTPDialogController ftpDialogController = new FTPDialogController(
                    ftpDialog, ftpInfoValidator, fsTypeSwitcher, statusBarController);

            ToolBarController toolBarController = new ToolBarController(
                    fsTypeSwitcher, ftpDialogController, dirTreeController, statusBarController);
            ToolBar toolBar = new ToolBar(toolBarController);

            mainWindow = new MainWindow("FsExplorer", toolBar, statusBar, dirTreePane, previewPane);
            MainWindowController mainWindowController = new MainWindowController(
                    mainWindow,
                    statusBarController,
                    disposables
            );
            mainWindow.setController(mainWindowController);

            applicationInitialized = true;
        } catch (IOException e) {
            errorMessage = DISK_ACCESS_ERROR;
            applicationInitialized = false;
        }
    }

    void run() {
        if (applicationInitialized) {
            SwingUtilities.invokeLater(mainWindow::show);
        } else {
            SwingUtilities.invokeLater(() -> {
                String msg = APP_START_ERROR;
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    msg += (": " + errorMessage);
                }
                JOptionPane.showMessageDialog(null, msg);
            });
        }
    }

    private List<PreviewRenderer> getPreivewRenderers() {
        List<PreviewRenderer> renderers = PreviewRenderersService.getRenderers();
        System.out.println("Loaded preview renderers number: " + renderers.size());
        return renderers;
    }
}
