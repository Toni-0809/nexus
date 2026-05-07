package com.nexus.controller;

// Конфигурация приложения: собирает ViewModel и зависимости
import com.nexus.ApplicationConfig;

// DTO блока для UI
import com.nexus.adapter.ui.viewmodel.BlockDto;

// Главная ViewModel экрана
import com.nexus.adapter.ui.viewmodel.MainViewModel;

// Сущность страницы
import com.nexus.model.entity.Page;

// Классы настроек интерфейса
import com.nexus.settings.SettingsDialog;
import com.nexus.settings.SettingsManager;
import com.nexus.settings.UiSettings;

// Таймер для автосохранения
import javafx.animation.PauseTransition;

// Запуск кода позже, когда UI уже построен
import javafx.application.Platform;

// Наблюдение за изменениями списка блоков
import javafx.collections.ListChangeListener;

// FXML-аннотация для связи с MainView.fxml
import javafx.fxml.FXML;

// Геометрия и выравнивание
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;

// Сцена JavaFX
import javafx.scene.Scene;

// Контролы интерфейса
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

// Эффекты и drag & drop
import javafx.scene.effect.DropShadow;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;

// Контейнеры layout
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

// Цвета и фигуры для карты связей
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

// Работа с окнами и файлами
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

// Время задержки
import javafx.util.Duration;

// Работа с файлами
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

// Коллекции
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Главный контроллер интерфейса Nexus.
 *
 * Его задача:
 * 1. Связать FXML-элементы с Java-кодом.
 * 2. Получать данные из MainViewModel.
 * 3. Реагировать на действия пользователя:
 *    - создать заметку
 *    - сохранить
 *    - импортировать / экспортировать
 *    - открыть карту
 *    - открыть настройки
 *    - удалить страницу или блок
 * 4. Перерисовывать блоки и дерево страниц.
 */
public class MainViewController {

    // -----------------------------
    // ЭЛЕМЕНТЫ ИНТЕРФЕЙСА ИЗ FXML
    // -----------------------------

    // Корневой контейнер окна
    @FXML
    private BorderPane rootPane;

    // Центральный контейнер, куда динамически рисуются блоки
    @FXML
    private VBox blocksContainer;

    // Поле поиска по страницам и блокам
    @FXML
    private TextField searchField;

    // Поле заголовка текущей страницы
    @FXML
    private TextField pageTitleField;

    // Кнопки нижней панели
    @FXML
    private Button newNoteButton;

    @FXML
    private Button saveButton;

    @FXML
    private Button importButton;

    @FXML
    private Button exportButton;

    @FXML
    private Button mapButton;

    @FXML
    private Button settingsButton;

    @FXML
    private Button deleteNoteButton;

    // Дерево страниц слева
    @FXML
    private TreeView<Page> pagesTree;

    // Надпись статуса сохранения: Saved / Unsaved / Autosaving...
    @FXML
    private Label saveStatusLabel;

    // -----------------------------
    // ВНУТРЕННИЕ ПОЛЯ КОНТРОЛЛЕРА
    // -----------------------------

    // ViewModel экрана
    private MainViewModel viewModel;

    // Таймер задержки для автосохранения
    private final PauseTransition autoSaveDelay = new PauseTransition(Duration.seconds(1.2));

    // Менеджер настроек UI
    private final SettingsManager settingsManager = new SettingsManager();

    // Текущие настройки интерфейса
    private UiSettings currentSettings;

    // -----------------------------
    // ИНИЦИАЛИЗАЦИЯ КОНТРОЛЛЕРА
    // -----------------------------

    /**
     * Вызывается автоматически после загрузки FXML.
     * Здесь мы:
     * - создаём ViewModel
     * - загружаем настройки
     * - настраиваем биндинги, дерево, действия кнопок
     * - рендерим текущее состояние
     * - ставим хоткеи
     */
    @FXML
    public void initialize() {
        viewModel = ApplicationConfig.createMainViewModel();
        currentSettings = settingsManager.load();

        bindStatus();
        bindPageTitle();
        bindSearch();
        configureAutoSave();
        configureTree();
        bindBlocks();
        bindActions();
        configureTooltips();

        refreshTree();
        selectCurrentPage();
        renderBlocks();

        Platform.runLater(() -> {
            applyUiSettings(currentSettings);
            installShortcuts();
            focusPageTitle();
        });
    }

    // -----------------------------
    // БИНДИНГИ (связь UI <-> ViewModel)
    // -----------------------------

    /**
     * Привязывает label статуса к ViewModel.
     * Если во ViewModel меняется saveState, надпись внизу тоже меняется.
     */
    private void bindStatus() {
        saveStatusLabel.textProperty().bind(viewModel.saveStateProperty());
    }

    /**
     * Привязывает поле заголовка страницы к ViewModel.
     * Изменение текста в поле сразу меняет title во ViewModel.
     */
    private void bindPageTitle() {
        pageTitleField.textProperty().bindBidirectional(viewModel.pageTitleProperty());
    }

    /**
     * Привязывает поле поиска к ViewModel.
     * При изменении строки поиска:
     * - обновляется дерево страниц
     * - выбирается текущая или первая подходящая страница
     * - перерисовываются блоки
     */
    private void bindSearch() {
        searchField.textProperty().bindBidirectional(viewModel.searchQueryProperty());

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            refreshTree();

            if (!selectCurrentPage()) {
                selectFirstFilteredPage();
            }

            renderBlocks();
        });
    }

    // -----------------------------
    // АВТОСОХРАНЕНИЕ
    // -----------------------------

    /**
     * Настраивает автосохранение.
     * Когда ViewModel сообщает, что страница изменилась,
     * запускается таймер. Если пользователь перестал печатать,
     * через 1.2 сек происходит автосохранение.
     */
    private void configureAutoSave() {
        autoSaveDelay.setOnFinished(event -> autoSaveNow());

        viewModel.changeVersionProperty().addListener((obs, oldValue, newValue) -> {
            if (viewModel.isDirty()) {
                autoSaveDelay.playFromStart();
            }
        });
    }

    /**
     * Выполняет автосохранение, если есть несохранённые изменения.
     */
    private void autoSaveNow() {
        if (!viewModel.isDirty()) {
            return;
        }

        try {
            viewModel.markSaving();
            viewModel.savePage();
            refreshTree();
            selectCurrentPage();
            renderBlocks();
        } catch (Exception ex) {
            error("Autosave", ex.getMessage());
        }
    }

    // -----------------------------
    // ЛЕВАЯ ПАНЕЛЬ: ДЕРЕВО СТРАНИЦ
    // -----------------------------

    /**
     * Настраивает TreeView слева.
     * Здесь:
     * - задаётся, как отображать Page в дереве
     * - задаётся логика выбора страницы
     */
    private void configureTree() {
        pagesTree.setShowRoot(true);

        // Настройка отображения каждой ячейки дерева
        pagesTree.setCellFactory(tree -> new TreeCell<>() {
            @Override
            protected void updateItem(Page item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitle());
                }
            }
        });

        // Что делать при выборе страницы слева
        pagesTree.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.getValue() == null) {
                return;
            }

            Page selectedPage = newValue.getValue();

            // Корневой узел "Structure" имеет id <= 0, его не открываем как страницу
            if (selectedPage.getId() > 0) {
                autoSaveDelay.stop();

                // Перед переключением сохраняем текущие несохранённые изменения
                if (viewModel.isDirty()) {
                    autoSaveNow();
                }

                viewModel.selectPage(selectedPage);
            }
        });
    }

    /**
     * Следит за изменениями списка блоков и перерисовывает их.
     */
    private void bindBlocks() {
        viewModel.getBlocks().addListener((ListChangeListener<BlockDto>) change -> renderBlocks());
    }

    // -----------------------------
    // ДЕЙСТВИЯ КНОПОК
    // -----------------------------

    /**
     * Назначает действия кнопкам нижней панели.
     */
    private void bindActions() {
        newNoteButton.setOnAction(event -> performNewNote());

        saveButton.setOnAction(event -> performSave(true));

        importButton.setOnAction(event -> performImport());

        exportButton.setOnAction(event -> performExport());

        mapButton.setOnAction(event -> {
            try {
                showMapWindow();
            } catch (Exception ex) {
                error("Map", ex.getMessage());
            }
        });

        settingsButton.setOnAction(event -> openSettings());

        deleteNoteButton.setOnAction(event -> {
            if (!confirm("Delete", "Delete current note?")) {
                return;
            }

            try {
                viewModel.deleteCurrentPage();
                refreshTree();
                selectCurrentPage();
                renderBlocks();
                information("Delete", "Note deleted.");
            } catch (Exception ex) {
                error("Delete", ex.getMessage());
            }
        });
    }

    // -----------------------------
    // ОСНОВНЫЕ ДЕЙСТВИЯ ПОЛЬЗОВАТЕЛЯ
    // -----------------------------

    /**
     * Создаёт новую заметку.
     * Перед этим сохраняет текущую, если она была изменена.
     */
    private void performNewNote() {
        autoSaveDelay.stop();

        if (viewModel.isDirty()) {
            autoSaveNow();
        }

        viewModel.createNewPage();
        refreshTree();
        selectCurrentPage();
        renderBlocks();

        Platform.runLater(this::focusPageTitle);
    }

    /**
     * Сохраняет текущую страницу.
     * @param showMessage показывать ли диалог "Save"
     */
    private void performSave(boolean showMessage) {
        try {
            autoSaveDelay.stop();
            viewModel.markSaving();
            viewModel.savePage();
            refreshTree();
            selectCurrentPage();
            renderBlocks();

            if (showMessage) {
                information("Save", "Structure updated.");
            }
        } catch (Exception ex) {
            error("Save", ex.getMessage());
        }
    }

    /**
     * Импортирует Markdown-файл как новую страницу.
     */
    private void performImport() {
        try {
            autoSaveDelay.stop();

            if (viewModel.isDirty()) {
                autoSaveNow();
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Import Markdown");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Markdown files", "*.md", "*.markdown")
            );

            File file = fileChooser.showOpenDialog(blocksContainer.getScene().getWindow());

            if (file == null) {
                return;
            }

            String markdown = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            viewModel.importMarkdown(markdown, baseNameWithoutExtension(file.getName()));

            searchField.clear();
            refreshTree();
            selectCurrentPage();
            renderBlocks();

            information("Import", "Markdown imported.");

            Platform.runLater(this::focusPageTitle);
        } catch (Exception ex) {
            error("Import", ex.getMessage());
        }
    }

    /**
     * Экспортирует текущую страницу в Markdown.
     */
    private void performExport() {
        try {
            autoSaveDelay.stop();

            if (viewModel.isDirty()) {
                autoSaveNow();
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Markdown");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Markdown files", "*.md")
            );
            fileChooser.setInitialFileName(suggestMarkdownFileName());

            File file = fileChooser.showSaveDialog(blocksContainer.getScene().getWindow());

            if (file == null) {
                return;
            }

            Files.writeString(
                    file.toPath(),
                    viewModel.exportCurrentPageAsMarkdown(),
                    StandardCharsets.UTF_8
            );

            information("Export", "Markdown exported.");
        } catch (Exception ex) {
            error("Export", ex.getMessage());
        }
    }

    // -----------------------------
    // ФОКУС / HOTKEYS / TOOLTIPS
    // -----------------------------

    /**
     * Ставит курсор в поле заголовка и выделяет текст.
     */
    private void focusPageTitle() {
        pageTitleField.requestFocus();
        pageTitleField.selectAll();
    }

    /**
     * Ставит курсор в поле поиска.
     */
    private void focusSearch() {
        searchField.requestFocus();
        searchField.selectAll();
    }

    /**
     * Назначает горячие клавиши.
     */
    private void installShortcuts() {
        Scene scene = blocksContainer.getScene();

        if (scene == null) {
            return;
        }

        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN),
                () -> performSave(true)
        );

        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN),
                this::performNewNote
        );

        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN),
                this::focusSearch
        );

        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN),
                this::performExport
        );

        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN),
                () -> {
                    try {
                        showMapWindow();
                    } catch (Exception ex) {
                        error("Map", ex.getMessage());
                    }
                }
        );
    }

    /**
     * Подсказки при наведении мыши на кнопки и поиск.
     */
    private void configureTooltips() {
        newNoteButton.setTooltip(new Tooltip("New Note (Ctrl+N)"));
        saveButton.setTooltip(new Tooltip("Save now (Ctrl+S)"));
        importButton.setTooltip(new Tooltip("Import Markdown"));
        exportButton.setTooltip(new Tooltip("Export Markdown (Ctrl+E)"));
        mapButton.setTooltip(new Tooltip("Open note map (Ctrl+M)"));
        deleteNoteButton.setTooltip(new Tooltip("Delete current note"));
        searchField.setTooltip(new Tooltip("Search pages and blocks (Ctrl+F)"));
        settingsButton.setTooltip(new Tooltip("Appearance settings"));
    }

    // -----------------------------
    // НАСТРОЙКИ ИНТЕРФЕЙСА
    // -----------------------------

    /**
     * Открывает окно настроек.
     * Если пользователь нажал Apply, сохраняет и применяет настройки.
     */
    private void openSettings() {
        SettingsDialog.show(blocksContainer.getScene().getWindow(), currentSettings)
                .ifPresent(settings -> {
                    currentSettings = settings;
                    settingsManager.save(settings);
                    applyUiSettings(settings);
                });
    }

    /**
     * Применяет тему, шрифт, размер и акцент к главному окну.
     */
    private void applyUiSettings(UiSettings settings) {
        rootPane.getStyleClass().removeAll("theme-dark", "theme-light");
        rootPane.getStyleClass().add(settings.themeStyleClass());
        rootPane.setStyle(settings.toInlineStyle());
    }

    // -----------------------------
    // ДЕРЕВО СТРАНИЦ: ОБНОВЛЕНИЕ
    // -----------------------------

    /**
     * Перестраивает дерево страниц слева.
     */
    private void refreshTree() {
        TreeItem<Page> root = new TreeItem<>(new Page(-1, "Structure"));
        root.setExpanded(true);

        for (Page page : viewModel.getFilteredPages()) {
            root.getChildren().add(new TreeItem<>(page));
        }

        pagesTree.setRoot(root);
    }

    /**
     * Пытается выделить текущую страницу в дереве.
     * @return true если выделение удалось
     */
    private boolean selectCurrentPage() {
        Page currentPage = viewModel.getCurrentPage();

        if (currentPage == null || pagesTree.getRoot() == null) {
            return false;
        }

        for (TreeItem<Page> child : pagesTree.getRoot().getChildren()) {
            Page page = child.getValue();

            if (page != null && page.getId() == currentPage.getId()) {
                pagesTree.getSelectionModel().select(child);
                return true;
            }
        }

        return false;
    }

    /**
     * Если текущая страница не попала в фильтр,
     * выбираем первую подходящую.
     */
    private void selectFirstFilteredPage() {
        if (pagesTree.getRoot() == null || pagesTree.getRoot().getChildren().isEmpty()) {
            return;
        }

        pagesTree.getSelectionModel().select(pagesTree.getRoot().getChildren().get(0));
    }

    // -----------------------------
    // РЕНДЕР БЛОКОВ
    // -----------------------------

    /**
     * Перерисовывает блоки текущей страницы в центре окна.
     */
    private void renderBlocks() {
        blocksContainer.getChildren().clear();

        List<BlockDto> visibleBlocks = viewModel.getFilteredBlocks();

        if (visibleBlocks.isEmpty()) {
            Label empty = new Label(
                    viewModel.hasActiveSearch()
                            ? "Nothing connected matches your search."
                            : "Connected thoughts begin here."
            );
            empty.getStyleClass().add("empty-state");
            blocksContainer.getChildren().add(empty);
            return;
        }

        for (BlockDto block : visibleBlocks) {
            blocksContainer.getChildren().add(createBlockNode(block));
        }
    }

    /**
     * Создаёт визуальную карточку одного блока.
     */
    private VBox createBlockNode(BlockDto block) {
        VBox card = new VBox(8);
        card.getStyleClass().add("block-card");

        // Мета-информация блока
        Label meta = new Label(createBlockMetaText(block));
        meta.getStyleClass().add("block-meta");

        // Ручка перетаскивания
        Label dragHandle = new Label("⋮⋮");
        dragHandle.getStyleClass().add("drag-handle");

        HBox header = new HBox(8, meta, dragHandle);
        header.getStyleClass().add("block-header");
        HBox.setHgrow(meta, Priority.ALWAYS);

        // Текстовый редактор блока
        TextArea editor = new TextArea();
        editor.getStyleClass().add("block-editor");
        editor.setWrapText(true);
        editor.setPrefHeight(130);
        editor.setText(block.getContent());

        // При изменении текста — обновляем DTO
        editor.textProperty().addListener((obs, oldText, newText) -> block.setContent(newText));

        // Контекстное меню
        ContextMenu contextMenu = new ContextMenu();
        MenuItem addLinkItem = new MenuItem("Add Link");
        addLinkItem.setOnAction(event -> addLink(block));
        contextMenu.getItems().add(addLinkItem);
        editor.setContextMenu(contextMenu);

        // Нижние кнопки карточки
        Button addBlockButton = new Button("+");
        addBlockButton.getStyleClass().add("subtle-button");
        addBlockButton.setOnAction(event -> viewModel.addParagraphBlock());

        Button addLinkButton = new Button("Add Link");
        addLinkButton.getStyleClass().add("subtle-button");
        addLinkButton.setOnAction(event -> addLink(block));

        Button deleteBlockButton = new Button("Delete");
        deleteBlockButton.getStyleClass().addAll("subtle-button", "danger-button");
        deleteBlockButton.setOnAction(event -> {
            if (!confirm("Delete", "Delete this block?")) {
                return;
            }

            viewModel.deleteBlock(block);
            renderBlocks();
        });

        HBox footer = new HBox(8, addBlockButton, addLinkButton, deleteBlockButton);
        footer.getStyleClass().add("block-footer");

        // Подключаем drag & drop
        configureBlockDragAndDrop(dragHandle, card, block);

        card.getChildren().addAll(header, editor, footer);
        VBox.setVgrow(editor, Priority.ALWAYS);

        return card;
    }

    // -----------------------------
    // DRAG & DROP БЛОКОВ
    // -----------------------------

    /**
     * Настраивает перетаскивание блока.
     */
    private void configureBlockDragAndDrop(Label dragHandle, VBox card, BlockDto block) {
        // Начало drag
        dragHandle.setOnDragDetected(event -> {
            if (viewModel.hasActiveSearch()) {
                information("Reorder", "Clear search before reordering blocks.");
                return;
            }

            int sourceIndex = viewModel.getBlocks().indexOf(block);

            if (sourceIndex < 0) {
                return;
            }

            Dragboard dragboard = dragHandle.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(sourceIndex));
            dragboard.setContent(content);

            event.consume();
        });

        // Перетаскивание над карточкой
        card.setOnDragOver(event -> {
            if (viewModel.hasActiveSearch()) {
                return;
            }

            Dragboard dragboard = event.getDragboard();

            if (!dragboard.hasString()) {
                return;
            }

            try {
                int sourceIndex = Integer.parseInt(dragboard.getString());
                int targetIndex = viewModel.getBlocks().indexOf(block);

                if (sourceIndex != targetIndex) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
            } catch (NumberFormatException ignored) {
            }

            event.consume();
        });

        // Визуальная подсветка цели
        card.setOnDragEntered(event -> {
            if (viewModel.hasActiveSearch()) {
                return;
            }

            Dragboard dragboard = event.getDragboard();

            if (dragboard.hasString() && !card.getStyleClass().contains("block-card-drop-target")) {
                card.getStyleClass().add("block-card-drop-target");
            }

            event.consume();
        });

        // Убираем подсветку
        card.setOnDragExited(event -> {
            card.getStyleClass().remove("block-card-drop-target");
            event.consume();
        });

        // Завершение drop
        card.setOnDragDropped(event -> {
            boolean success = false;

            if (!viewModel.hasActiveSearch()) {
                Dragboard dragboard = event.getDragboard();

                if (dragboard.hasString()) {
                    try {
                        int sourceIndex = Integer.parseInt(dragboard.getString());
                        int targetIndex = viewModel.getBlocks().indexOf(block);

                        if (sourceIndex != targetIndex && sourceIndex >= 0 && targetIndex >= 0) {
                            int insertIndex = sourceIndex < targetIndex ? targetIndex : targetIndex + 1;

                            viewModel.moveBlock(sourceIndex, insertIndex);
                            renderBlocks();
                            success = true;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            card.getStyleClass().remove("block-card-drop-target");
            event.setDropCompleted(success);
            event.consume();
        });

        dragHandle.setOnDragDone(event -> event.consume());
    }

    /**
     * Создаёт строку мета-информации блока.
     */
    private String createBlockMetaText(BlockDto block) {
        if (block.getId() <= 0) {
            return "Block draft";
        }

        int backlinksCount = viewModel.getBacklinksCount(block.getId());
        return "Block #" + block.getId() + " • Backlinks " + backlinksCount;
    }

    // -----------------------------
    // ССЫЛКИ МЕЖДУ БЛОКАМИ
    // -----------------------------

    /**
     * Открывает диалог выбора целевого блока и создаёт ссылку.
     */
    private void addLink(BlockDto sourceBlock) {
        if (sourceBlock.getId() <= 0) {
            error("Add Link", "Save the page before creating links.");
            return;
        }

        List<BlockDto> candidates = viewModel.getLinkableBlocks(sourceBlock.getId());

        if (candidates.isEmpty()) {
            information("Add Link", "No available saved blocks to connect.");
            return;
        }

        List<String> choices = candidates.stream()
                .map(this::formatBlockChoice)
                .toList();

        ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
        dialog.setTitle("Add Link");
        dialog.setHeaderText("Connect this block to another block.");
        dialog.setContentText("Target block:");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(selected -> {
            try {
                long targetBlockId = extractBlockId(selected);
                viewModel.createLink(sourceBlock.getId(), targetBlockId);
                renderBlocks();
                information("Connected", "Link created.");
            } catch (Exception ex) {
                error("Add Link", ex.getMessage());
            }
        });
    }

    /**
     * Делает красивую строку для списка выбора блока.
     */
    private String formatBlockChoice(BlockDto block) {
        String content = block.getContent() == null ? "" : block.getContent().trim();

        if (content.isBlank()) {
            content = "(empty block)";
        }

        if (content.length() > 40) {
            content = content.substring(0, 40) + "...";
        }

        return "Block #" + block.getId() + " — " + content;
    }

    /**
     * Из строки вида "Block #12 — text"
     * вытаскивает число 12.
     */
    private long extractBlockId(String choice) {
        int hashIndex = choice.indexOf('#');
        int dashIndex = choice.indexOf('—');

        if (hashIndex < 0 || dashIndex < 0 || dashIndex <= hashIndex) {
            throw new IllegalArgumentException("Invalid block selection.");
        }

        String idText = choice.substring(hashIndex + 1, dashIndex).trim();
        return Long.parseLong(idText);
    }

    // -----------------------------
    // КАРТА СВЯЗЕЙ
    // -----------------------------

    /**
     * Открывает отдельное окно карты связей текущей заметки.
     */
    private void showMapWindow() {
        List<BlockDto> mapBlocks = List.copyOf(viewModel.getBlocks());

        if (mapBlocks.isEmpty()) {
            information("Map", "No blocks to display.");
            return;
        }

        Stage mapStage = new Stage();
        mapStage.initOwner(blocksContainer.getScene().getWindow());
        mapStage.setTitle("Nexus Map");

        BorderPane root = new BorderPane();
        root.getStyleClass().add(currentSettings.themeStyleClass());
        root.setStyle(currentSettings.toInlineStyle());

        Label titleLabel = new Label("Structure Map — " + viewModel.getPageTitle());
        titleLabel.getStyleClass().add("map-title-label");

        Label infoLabel = new Label("Current note structure.");
        infoLabel.getStyleClass().add("map-info-label");

        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("subtle-button");

        ToolBar mapToolbar = new ToolBar(titleLabel, refreshButton, infoLabel);
        mapToolbar.getStyleClass().add("bottom-toolbar");

        Pane graphPane = new Pane();
        graphPane.getStyleClass().add("map-pane");
        graphPane.setPrefSize(860, 560);

        refreshButton.setOnAction(event -> renderMap(graphPane, infoLabel));

        root.setTop(mapToolbar);
        root.setCenter(graphPane);

        Scene scene = new Scene(root, 960, 680);
        scene.getStylesheets().add(Objects.requireNonNull(
                MainViewController.class.getResource("/css/styles.css"),
                "styles.css not found"
        ).toExternalForm());

        mapStage.setScene(scene);
        mapStage.show();

        renderMap(graphPane, infoLabel);
    }

    /**
     * Рисует карту:
     * - узлы = блоки
     * - линии = ссылки
     */
    private void renderMap(Pane graphPane, Label infoLabel) {
        graphPane.getChildren().clear();

        List<BlockDto> mapBlocks = List.copyOf(viewModel.getBlocks());

        if (mapBlocks.isEmpty()) {
            infoLabel.setText("No blocks in current note.");
            return;
        }

        double width = Math.max(graphPane.getWidth(), graphPane.getPrefWidth());
        double height = Math.max(graphPane.getHeight(), graphPane.getPrefHeight());

        Map<BlockDto, Point2D> positions = calculateNodePositions(mapBlocks, width, height);
        Map<Long, BlockDto> blockById = new HashMap<>();
        Set<Long> pageBlockIds = new HashSet<>();

        for (BlockDto block : mapBlocks) {
            if (block.getId() > 0) {
                blockById.put(block.getId(), block);
                pageBlockIds.add(block.getId());
            }
        }

        int edgeCount = 0;

        // Сначала рисуем линии
        for (BlockDto sourceBlock : mapBlocks) {
            if (sourceBlock.getId() <= 0) {
                continue;
            }

            Point2D start = positions.get(sourceBlock);

            for (Long targetId : viewModel.getOutgoingTargetIds(sourceBlock.getId())) {
                if (!pageBlockIds.contains(targetId)) {
                    continue;
                }

                BlockDto targetBlock = blockById.get(targetId);
                Point2D end = positions.get(targetBlock);

                if (start == null || end == null) {
                    continue;
                }

                Line line = new Line(start.getX(), start.getY(), end.getX(), end.getY());
                line.setStroke(Color.web("#5B7FA6"));
                line.setStrokeWidth(2);

                graphPane.getChildren().add(line);
                edgeCount++;
            }
        }

        // Потом рисуем узлы поверх линий
        for (int i = 0; i < mapBlocks.size(); i++) {
            BlockDto block = mapBlocks.get(i);
            Point2D point = positions.get(block);

            if (point == null) {
                continue;
            }

            Circle circle = new Circle(42);
            circle.setFill(Color.web("#25304A"));
            circle.setStroke(Color.web("#5B7FA6"));
            circle.setStrokeWidth(2);

            Label nodeLabel = new Label(formatMapNodeLabel(block, i + 1));
            nodeLabel.getStyleClass().add("map-node-label");
            nodeLabel.setWrapText(true);
            nodeLabel.setMaxWidth(84);

            StackPane node = new StackPane(circle, nodeLabel);
            node.setLayoutX(point.getX() - 42);
            node.setLayoutY(point.getY() - 42);

            Tooltip.install(node, new Tooltip(buildMapTooltip(block, i + 1)));

            graphPane.getChildren().add(node);
        }

        infoLabel.setText("Blocks: " + mapBlocks.size() + " • Links: " + edgeCount);
    }

    /**
     * Вычисляет координаты узлов по кругу.
     */
    private Map<BlockDto, Point2D> calculateNodePositions(List<BlockDto> blocks, double width, double height) {
        Map<BlockDto, Point2D> positions = new HashMap<>();

        double centerX = width / 2;
        double centerY = height / 2;

        if (blocks.size() == 1) {
            positions.put(blocks.get(0), new Point2D(centerX, centerY));
            return positions;
        }

        double radius = Math.max(140, Math.min(width, height) * 0.32);

        for (int i = 0; i < blocks.size(); i++) {
            double angle = (2 * Math.PI * i / blocks.size()) - (Math.PI / 2);
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);

            positions.put(blocks.get(i), new Point2D(x, y));
        }

        return positions;
    }

    /**
     * Короткий текст внутри узла карты.
     */
    private String formatMapNodeLabel(BlockDto block, int localNumber) {
        String content = block.getContent() == null ? "" : block.getContent().trim();

        if (content.isBlank()) {
            content = "(empty)";
        }

        if (content.length() > 22) {
            content = content.substring(0, 22) + "...";
        }

        return localNumber + "\n" + content;
    }

    /**
     * Полный tooltip узла карты.
     */
    private String buildMapTooltip(BlockDto block, int localNumber) {
        String content = block.getContent() == null ? "" : block.getContent().trim();

        if (content.isBlank()) {
            content = "(empty block)";
        }

        return "Block " + localNumber + "\n\n" + content;
    }

    // -----------------------------
    // РАБОТА С ИМЕНАМИ ФАЙЛОВ
    // -----------------------------

    /**
     * Убирает расширение файла.
     */
    private String baseNameWithoutExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "Imported Note";
        }

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }

        return fileName;
    }

    /**
     * Предлагает безопасное имя markdown-файла для экспорта.
     */
    private String suggestMarkdownFileName() {
        String title = viewModel.getPageTitle();

        if (title == null || title.isBlank()) {
            title = "nexus-note";
        }

        String safe = title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();

        if (safe.isBlank()) {
            safe = "nexus-note";
        }

        return safe + ".md";
    }

    // -----------------------------
    // КРАСИВЫЕ КАСТОМНЫЕ ДИАЛОГИ
    // -----------------------------

    /**
     * Информационный диалог.
     */
    private void information(String title, String message) {
        showMessageDialog(title, message, "Info", "nexus-dialog-badge-info");
    }

    /**
     * Диалог ошибки.
     */
    private void error(String title, String message) {
        showMessageDialog(title, message, "Error", "nexus-dialog-badge-error");
    }

    /**
     * Диалог подтверждения.
     * Возвращает true, если пользователь нажал Delete.
     */
    private boolean confirm(String title, String message) {
        Stage dialog = createDialogStage();

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("nexus-dialog-title");

        Label badgeLabel = new Label("Confirm");
        badgeLabel.getStyleClass().addAll("nexus-dialog-badge", "nexus-dialog-badge-confirm");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, titleLabel, spacer, badgeLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("nexus-dialog-header");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("nexus-dialog-message");

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().addAll("subtle-button", "nexus-dialog-button");

        Button confirmButton = new Button("Delete");
        confirmButton.getStyleClass().addAll("danger-button", "nexus-dialog-button");
        confirmButton.setDefaultButton(true);
        cancelButton.setCancelButton(true);

        final boolean[] confirmed = {false};

        cancelButton.setOnAction(event -> dialog.close());
        confirmButton.setOnAction(event -> {
            confirmed[0] = true;
            dialog.close();
        });

        HBox actions = new HBox(10, cancelButton, confirmButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("nexus-dialog-actions");

        VBox card = new VBox(18, header, messageLabel, actions);
        card.getStyleClass().add("nexus-dialog-card");
        card.setMaxWidth(420);
        card.setEffect(new DropShadow(30, Color.rgb(0, 0, 0, 0.35)));

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(18));
        root.getStyleClass().add("nexus-dialog-root");

        Scene scene = createDialogScene(root);
        dialog.setScene(scene);
        dialog.showAndWait();

        return confirmed[0];
    }

    /**
     * Универсальный красивый диалог-сообщение.
     */
    private void showMessageDialog(String title, String message, String badgeText, String badgeStyleClass) {
        Stage dialog = createDialogStage();

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("nexus-dialog-title");

        Label badgeLabel = new Label(badgeText);
        badgeLabel.getStyleClass().addAll("nexus-dialog-badge", badgeStyleClass);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, titleLabel, spacer, badgeLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("nexus-dialog-header");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("nexus-dialog-message");

        Button okButton = new Button("OK");
        okButton.getStyleClass().addAll("nexus-dialog-button", "nexus-dialog-primary-button");
        okButton.setDefaultButton(true);
        okButton.setOnAction(event -> dialog.close());

        HBox actions = new HBox(okButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("nexus-dialog-actions");

        VBox card = new VBox(18, header, messageLabel, actions);
        card.getStyleClass().add("nexus-dialog-card");
        card.setMaxWidth(420);
        card.setEffect(new DropShadow(30, Color.rgb(0, 0, 0, 0.35)));

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(18));
        root.getStyleClass().add("nexus-dialog-root");

        Scene scene = createDialogScene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /**
     * Создаёт модальное окно для кастомного диалога.
     */
    private Stage createDialogStage() {
        Stage dialog = new Stage();
        dialog.initOwner(blocksContainer.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setResizable(false);
        return dialog;
    }

    /**
     * Создаёт сцену для кастомного диалога,
     * применяет тему и CSS.
     */
    private Scene createDialogScene(StackPane root) {
        root.getStyleClass().add(currentSettings.themeStyleClass());
        root.setStyle(currentSettings.toInlineStyle());

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/css/styles.css"),
                        "styles.css not found"
                ).toExternalForm()
        );

        return scene;
    }
}
