package com.nexus.adapter.ui.viewmodel;

// Сущности ядра
import com.nexus.model.Link;
import com.nexus.model.entity.Block;
import com.nexus.model.entity.Page;

// Use cases приложения
import com.nexus.usecase.CreateLinkUseCase;
import com.nexus.usecase.CreatePageUseCase;
import com.nexus.usecase.GetBacklinksForBlockUseCase;
import com.nexus.usecase.GetBlocksForPageUseCase;
import com.nexus.usecase.GetOutgoingLinksForBlockUseCase;

// Порты репозиториев
import com.nexus.usecase.port.BlockRepository;
import com.nexus.usecase.port.PageRepository;

// JavaFX properties для MVVM
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

// ObservableList для автоматического обновления UI
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

// Утилиты Java
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MainViewModel — это слой между интерфейсом и бизнес-логикой.
 *
 * Проще говоря:
 * - Controller общается с пользователем
 * - ViewModel хранит состояние экрана и команды
 * - Use cases и repositories выполняют настоящую работу
 *
 * Здесь хранится:
 * - текущая страница
 * - список страниц
 * - список блоков текущей страницы
 * - строка поиска
 * - состояние сохранения
 *
 * И здесь же есть команды:
 * - создать новую страницу
 * - выбрать страницу
 * - добавить блок
 * - переместить блок
 * - сохранить
 * - удалить
 * - создать ссылку
 * - экспортировать markdown
 * - импортировать markdown
 */
public class MainViewModel {

    // -----------------------------
    // ЗАВИСИМОСТИ VIEWMODEL
    // -----------------------------
    // Это use cases и репозитории, через которые ViewModel работает с данными.

    private final CreatePageUseCase createPageUseCase;
    private final GetBlocksForPageUseCase getBlocksForPageUseCase;
    private final CreateLinkUseCase createLinkUseCase;
    private final GetBacklinksForBlockUseCase getBacklinksForBlockUseCase;
    private final GetOutgoingLinksForBlockUseCase getOutgoingLinksForBlockUseCase;
    private final PageRepository pageRepository;
    private final BlockRepository blockRepository;

    // -----------------------------
    // СОСТОЯНИЕ ЭКРАНА
    // -----------------------------

    // Заголовок текущей страницы
    private final StringProperty pageTitle = new SimpleStringProperty("Connected");

    // Строка поиска
    private final StringProperty searchQuery = new SimpleStringProperty("");

    // Есть ли несохранённые изменения
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);

    // Текстовый статус сохранения: Saved / Unsaved / Autosaving...
    private final StringProperty saveState = new SimpleStringProperty("Saved");

    // Счётчик изменений.
    // Когда что-то меняется, он увеличивается.
    // Controller использует это, чтобы запускать автосохранение.
    private final LongProperty changeVersion = new SimpleLongProperty(0);

    // Список блоков текущей страницы
    private final ObservableList<BlockDto> blocks = FXCollections.observableArrayList();

    // Список всех страниц
    private final ObservableList<Page> pages = FXCollections.observableArrayList();

    // Набор блоков, на которые уже навешаны слушатели изменений.
    // Нужен, чтобы не навешивать слушатели повторно.
    private final Set<BlockDto> observedBlocks = Collections.newSetFromMap(new IdentityHashMap<>());

    // Текущая выбранная страница
    private Page currentPage;

    // Флаг: временно отключить отслеживание dirty
    // Используется, когда мы загружаем данные из базы и не хотим считать это "ручным изменением пользователя"
    private boolean suppressDirtyTracking = false;

    // -----------------------------
    // КОНСТРУКТОР
    // -----------------------------

    public MainViewModel(CreatePageUseCase createPageUseCase,
                         GetBlocksForPageUseCase getBlocksForPageUseCase,
                         CreateLinkUseCase createLinkUseCase,
                         GetBacklinksForBlockUseCase getBacklinksForBlockUseCase,
                         GetOutgoingLinksForBlockUseCase getOutgoingLinksForBlockUseCase,
                         PageRepository pageRepository,
                         BlockRepository blockRepository) {
        this.createPageUseCase = createPageUseCase;
        this.getBlocksForPageUseCase = getBlocksForPageUseCase;
        this.createLinkUseCase = createLinkUseCase;
        this.getBacklinksForBlockUseCase = getBacklinksForBlockUseCase;
        this.getOutgoingLinksForBlockUseCase = getOutgoingLinksForBlockUseCase;
        this.pageRepository = pageRepository;
        this.blockRepository = blockRepository;

        // Подключаем наблюдение за изменениями
        observeChanges();

        // Загружаем страницы из базы
        loadPages();

        // Если страниц нет — создаём первую.
        // Если есть — выбираем первую страницу.
        if (pages.isEmpty()) {
            createNewPage();
        } else {
            selectPage(pages.get(0));
        }
    }

    // -----------------------------
    // PROPERTIES ДЛЯ UI
    // -----------------------------

    public StringProperty pageTitleProperty() {
        return pageTitle;
    }

    public String getPageTitle() {
        return pageTitle.get();
    }

    public void setPageTitle(String title) {
        pageTitle.set(title);

        // Если текущая страница уже есть,
        // сразу обновляем title в сущности Page
        if (currentPage != null) {
            currentPage.setTitle(title);
        }
    }

    public StringProperty searchQueryProperty() {
        return searchQuery;
    }

    public BooleanProperty dirtyProperty() {
        return dirty;
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public StringProperty saveStateProperty() {
        return saveState;
    }

    public LongProperty changeVersionProperty() {
        return changeVersion;
    }

    /**
     * Вызывается перед сохранением, чтобы UI показал "Autosaving..."
     */
    public void markSaving() {
        saveState.set("Autosaving...");
    }

    // -----------------------------
    // ПОИСК И ФИЛЬТРАЦИЯ
    // -----------------------------

    /**
     * Есть ли активный поиск.
     */
    public boolean hasActiveSearch() {
        return !normalizedSearchQuery().isBlank();
    }

    /**
     * Возвращает страницы с учётом поиска.
     * Если поиск пустой — возвращает все страницы.
     */
    public List<Page> getFilteredPages() {
        String query = normalizedSearchQuery();

        if (query.isBlank()) {
            return List.copyOf(pages);
        }

        return pages.stream()
                .filter(page -> pageMatchesQuery(page, query))
                .toList();
    }

    /**
     * Возвращает блоки текущей страницы с учётом поиска.
     * Если поиск пустой — возвращает все блоки.
     */
    public List<BlockDto> getFilteredBlocks() {
        String query = normalizedSearchQuery();

        if (query.isBlank()) {
            return List.copyOf(blocks);
        }

        return blocks.stream()
                .filter(block -> blockMatchesQuery(block, query))
                .toList();
    }

    /**
     * Видна ли текущая страница в отфильтрованном списке.
     */
    public boolean isCurrentPageVisibleInSearch() {
        if (currentPage == null) {
            return false;
        }

        return getFilteredPages().stream()
                .anyMatch(page -> page.getId() == currentPage.getId());
    }

    // -----------------------------
    // ДОСТУП К ДАННЫМ ДЛЯ VIEW
    // -----------------------------

    public ObservableList<BlockDto> getBlocks() {
        return blocks;
    }

    public ObservableList<Page> getPages() {
        return pages;
    }

    public Page getCurrentPage() {
        return currentPage;
    }

    // -----------------------------
    // ЗАГРУЗКА И ВЫБОР СТРАНИЦЫ
    // -----------------------------

    /**
     * Загружает все страницы из репозитория.
     */
    public void loadPages() {
        pages.setAll(pageRepository.findAll());
    }

    /**
     * Выбирает страницу и загружает её блоки.
     */
    public void selectPage(Page page) {
        if (page == null) {
            return;
        }

        // Во время программной загрузки не считаем изменения "грязными"
        runWithoutDirtyTracking(() -> {
            currentPage = page;
            pageTitle.set(page.getTitle());

            List<Block> loadedBlocks = getBlocksForPageUseCase.execute(page.getId());

            // Очищаем список уже наблюдаемых блоков
            observedBlocks.clear();

            // Переводим Block -> BlockDto
            blocks.setAll(loadedBlocks.stream().map(BlockDto::from).toList());

            // Если страница пустая — добавляем один пустой блок
            if (blocks.isEmpty()) {
                blocks.add(new BlockDto(
                        0,
                        page.getId(),
                        Block.BlockType.PARAGRAPH,
                        "",
                        0
                ));
            }
        });

        markSaved();
    }

    /**
     * Создаёт новую страницу.
     */
    public void createNewPage() {
        Page newPage = createPageUseCase.execute("New Note");

        loadPages();

        runWithoutDirtyTracking(() -> {
            currentPage = newPage;
            pageTitle.set(newPage.getTitle());

            observedBlocks.clear();
            blocks.clear();

            // Новая страница сразу получает один пустой блок
            blocks.add(new BlockDto(
                    0,
                    newPage.getId(),
                    Block.BlockType.PARAGRAPH,
                    "",
                    0
            ));
        });

        markSaved();
    }

    // -----------------------------
    // РАБОТА С БЛОКАМИ
    // -----------------------------

    /**
     * Добавляет новый пустой параграфный блок.
     */
    public void addParagraphBlock() {
        long pageId = currentPage != null ? currentPage.getId() : 0;

        blocks.add(new BlockDto(
                0,
                pageId,
                Block.BlockType.PARAGRAPH,
                "",
                blocks.size()
        ));
    }

    /**
     * Перемещает блок из одной позиции в другую.
     * Используется drag & drop.
     */
    public void moveBlock(int sourceIndex, int targetIndex) {
        if (sourceIndex < 0 || sourceIndex >= blocks.size()) {
            return;
        }

        if (targetIndex < 0 || targetIndex > blocks.size()) {
            return;
        }

        if (sourceIndex == targetIndex) {
            return;
        }

        BlockDto moved = blocks.remove(sourceIndex);

        if (targetIndex > blocks.size()) {
            targetIndex = blocks.size();
        }

        blocks.add(targetIndex, moved);

        // После перестановки обновляем orderIndex
        for (int i = 0; i < blocks.size(); i++) {
            blocks.get(i).setOrderIndex(i);
        }
    }

    /**
     * Сохраняет текущую страницу и все её блоки.
     */
    public void savePage() {
        if (currentPage == null) {
            createNewPage();
        }

        runWithoutDirtyTracking(() -> {
            currentPage.setTitle(pageTitle.get());
            pageRepository.save(currentPage);

            for (int i = 0; i < blocks.size(); i++) {
                BlockDto dto = blocks.get(i);
                dto.setPageId(currentPage.getId());
                dto.setOrderIndex(i);

                Block savedBlock = blockRepository.save(dto.toEntity());

                // После save у блока появляется настоящий id из БД
                dto.setId(savedBlock.getId());
                dto.setPageId(savedBlock.getPageId());
                dto.setOrderIndex(savedBlock.getOrderIndex());
            }

            loadPages();
        });

        markSaved();
    }

    /**
     * Удаляет один блок.
     */
    public void deleteBlock(BlockDto block) {
        if (block == null) {
            return;
        }

        if (block.getId() > 0) {
            blockRepository.deleteById(block.getId());
        }

        blocks.remove(block);

        // Пересчитываем порядок
        for (int i = 0; i < blocks.size(); i++) {
            blocks.get(i).setOrderIndex(i);
        }

        // Если все блоки удалены — оставляем один пустой
        if (blocks.isEmpty()) {
            addParagraphBlock();
        }
    }

    /**
     * Удаляет текущую страницу.
     */
    public void deleteCurrentPage() {
        if (currentPage == null) {
            return;
        }

        if (currentPage.getId() > 0) {
            pageRepository.deleteById(currentPage.getId());
        }

        loadPages();

        if (pages.isEmpty()) {
            createNewPage();
        } else {
            selectPage(pages.get(0));
        }

        markSaved();
    }

    // -----------------------------
    // ССЫЛКИ МЕЖДУ БЛОКАМИ
    // -----------------------------

    /**
     * Возвращает список блоков, в которые можно создать ссылку.
     */
    public List<BlockDto> getLinkableBlocks(long sourceBlockId) {
        return blocks.stream()
                .filter(block -> block.getId() > 0)
                .filter(block -> block.getId() != sourceBlockId)
                .toList();
    }

    /**
     * Создаёт ссылку между двумя блоками.
     */
    public void createLink(long sourceBlockId, long targetBlockId) {
        createLinkUseCase.execute(sourceBlockId, targetBlockId);
    }

    /**
     * Сколько backlink'ов ведёт в данный блок.
     */
    public int getBacklinksCount(long targetBlockId) {
        return getBacklinksForBlockUseCase.execute(targetBlockId).size();
    }

    /**
     * Возвращает id блоков, на которые ссылается данный блок.
     * Нужно для карты связей.
     */
    public List<Long> getOutgoingTargetIds(long sourceBlockId) {
        return getOutgoingLinksForBlockUseCase.execute(sourceBlockId).stream()
                .map(Link::getTargetBlockId)
                .toList();
    }

    // -----------------------------
    // ЭКСПОРТ MARKDOWN
    // -----------------------------

    /**
     * Экспортирует текущую страницу в Markdown.
     */
    public String exportCurrentPageAsMarkdown() {
        String title = pageTitle.get() == null || pageTitle.get().isBlank()
                ? "Untitled"
                : pageTitle.get().trim();

        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(title).append("\n\n");
        markdown.append("> Exported from Nexus\n\n");

        List<BlockDto> exportBlocks = List.copyOf(blocks);

        // Сопоставление: database id блока -> локальный номер блока в файле
        Map<Long, Integer> localNumberByBlockId = new HashMap<>();

        for (int i = 0; i < exportBlocks.size(); i++) {
            BlockDto block = exportBlocks.get(i);
            if (block.getId() > 0) {
                localNumberByBlockId.put(block.getId(), i + 1);
            }
        }

        if (exportBlocks.isEmpty()) {
            markdown.append("_No blocks_\n");
            return markdown.toString();
        }

        for (int i = 0; i < exportBlocks.size(); i++) {
            BlockDto block = exportBlocks.get(i);
            int localNumber = i + 1;

            // Скрытый комментарий для импорта обратно
            markdown.append("<!-- nexus:block:").append(localNumber).append(" -->\n");

            String content = block.getContent() == null ? "" : block.getContent().trim();

            if (content.isBlank()) {
                markdown.append("_empty block_").append("\n");
            } else {
                markdown.append(content).append("\n");
            }

            // Исходящие ссылки
            if (block.getId() > 0) {
                List<Link> outgoingLinks = getOutgoingLinksForBlockUseCase.execute(block.getId());

                List<String> localLinks = outgoingLinks.stream()
                        .map(Link::getTargetBlockId)
                        .filter(localNumberByBlockId::containsKey)
                        .map(targetId -> "[[Block " + localNumberByBlockId.get(targetId) + "]]")
                        .toList();

                if (!localLinks.isEmpty()) {
                    markdown.append("\n");
                    markdown.append("Links: ").append(String.join(", ", localLinks)).append("\n");
                }

                int backlinks = getBacklinksCount(block.getId());
                if (backlinks > 0) {
                    markdown.append("\n");
                    markdown.append("Backlinks: ").append(backlinks).append("\n");
                }
            }

            if (i < exportBlocks.size() - 1) {
                markdown.append("\n---\n\n");
            }
        }

        markdown.append("\n");
        return markdown.toString();
    }

    // -----------------------------
    // ИМПОРТ MARKDOWN
    // -----------------------------

    /**
     * Импортирует markdown как новую страницу.
     */
    public void importMarkdown(String markdownContent, String fallbackTitle) {
        String normalized = normalizeMarkdown(markdownContent);
        String importedTitle = extractImportedTitle(normalized, fallbackTitle);
        List<ImportedBlock> importedBlocks = parseImportedBlocks(normalized);

        Page newPage = createPageUseCase.execute(importedTitle);
        loadPages();

        runWithoutDirtyTracking(() -> {
            currentPage = newPage;
            pageTitle.set(importedTitle);
            observedBlocks.clear();
            blocks.clear();

            if (importedBlocks.isEmpty()) {
                blocks.add(new BlockDto(
                        0,
                        newPage.getId(),
                        Block.BlockType.PARAGRAPH,
                        "",
                        0
                ));
            } else {
                for (int i = 0; i < importedBlocks.size(); i++) {
                    ImportedBlock importedBlock = importedBlocks.get(i);

                    blocks.add(new BlockDto(
                            0,
                            newPage.getId(),
                            Block.BlockType.PARAGRAPH,
                            importedBlock.content(),
                            i
                    ));
                }
            }
        });

        savePage();
        recreateImportedLinks(importedBlocks);
        markSaved();
    }

    /**
     * После импорта создаёт ссылки между блоками.
     */
    private void recreateImportedLinks(List<ImportedBlock> importedBlocks) {
        Map<Integer, Long> importedIdByLocalNumber = new HashMap<>();

        for (int i = 0; i < blocks.size(); i++) {
            BlockDto block = blocks.get(i);

            if (block.getId() > 0) {
                importedIdByLocalNumber.put(i + 1, block.getId());
            }
        }

        for (int i = 0; i < importedBlocks.size() && i < blocks.size(); i++) {
            BlockDto sourceBlock = blocks.get(i);

            if (sourceBlock.getId() <= 0) {
                continue;
            }

            for (Integer targetLocalNumber : importedBlocks.get(i).localTargets()) {
                Long targetId = importedIdByLocalNumber.get(targetLocalNumber);

                if (targetId != null && targetId != sourceBlock.getId()) {
                    createLinkUseCase.execute(sourceBlock.getId(), targetId);
                }
            }
        }
    }

    /**
     * Нормализует переводы строк.
     */
    private String normalizeMarkdown(String markdownContent) {
        if (markdownContent == null) {
            return "";
        }

        return markdownContent
                .replace("\r\n", "\n")
                .replace("\r", "\n");
    }

    /**
     * Вытаскивает заголовок страницы из markdown.
     */
    private String extractImportedTitle(String markdown, String fallbackTitle) {
        Pattern titlePattern = Pattern.compile("(?m)^#\\s+(.+?)\\s*$");
        Matcher matcher = titlePattern.matcher(markdown);

        if (matcher.find()) {
            String title = matcher.group(1).trim();
            if (!title.isBlank()) {
                return title;
            }
        }

        if (fallbackTitle == null || fallbackTitle.isBlank()) {
            return "Imported Note";
        }

        return fallbackTitle.trim();
    }

    /**
     * Делит markdown на блоки.
     */
    private List<ImportedBlock> parseImportedBlocks(String markdown) {
        String body = markdown;

        body = body.replaceFirst("(?m)^#\\s+.+?\\s*$\\n?", "");
        body = body.replaceFirst("(?m)^>\\s*Exported from Nexus\\s*$\\n?", "");

        String[] sections = body.split("(?m)^---\\s*$");
        List<ImportedBlock> result = new ArrayList<>();

        for (String section : sections) {
            ImportedBlock importedBlock = parseImportedBlock(section);

            if (importedBlock != null) {
                result.add(importedBlock);
            }
        }

        return result;
    }

    /**
     * Разбирает один импортируемый блок.
     */
    private ImportedBlock parseImportedBlock(String rawSection) {
        String section = rawSection == null ? "" : rawSection.trim();

        if (section.isBlank()) {
            return null;
        }

        String[] lines = section.split("\n");
        List<String> contentLines = new ArrayList<>();
        List<Integer> localTargets = new ArrayList<>();

        Pattern localLinkPattern = Pattern.compile("\\[\\[Block\\s+(\\d+)]]");

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("<!-- nexus:block:")) {
                continue;
            }

            if (trimmed.startsWith("Links:")) {
                Matcher matcher = localLinkPattern.matcher(trimmed);
                while (matcher.find()) {
                    localTargets.add(Integer.parseInt(matcher.group(1)));
                }
                continue;
            }

            if (trimmed.startsWith("Backlinks:")) {
                continue;
            }

            contentLines.add(line);
        }

        String content = String.join("\n", contentLines).trim();

        if (content.equals("_empty block_")) {
            content = "";
        }

        return new ImportedBlock(content, localTargets);
    }

    // -----------------------------
    // ПОИСК
    // -----------------------------

    /**
     * Проверяет, подходит ли страница под запрос поиска.
     * Страница подходит, если:
     * - запрос найден в title
     * - или запрос найден в одном из блоков страницы
     */
    private boolean pageMatchesQuery(Page page, String query) {
        if (page.getTitle().toLowerCase().contains(query)) {
            return true;
        }

        return blockRepository.findByPageId(page.getId()).stream()
                .anyMatch(block -> blockMatchesQuery(block, query));
    }

    /**
     * Проверяет, подходит ли Block под поиск.
     */
    private boolean blockMatchesQuery(Block block, String query) {
        String content = block.getContent() == null ? "" : block.getContent().toLowerCase();
        String type = block.getType().name().toLowerCase();

        return content.contains(query) || type.contains(query);
    }

    /**
     * Проверяет, подходит ли BlockDto под поиск.
     */
    private boolean blockMatchesQuery(BlockDto block, String query) {
        String content = block.getContent() == null ? "" : block.getContent().toLowerCase();
        String type = block.getType().name().toLowerCase();

        return content.contains(query) || type.contains(query);
    }

    /**
     * Нормализует строку поиска.
     */
    private String normalizedSearchQuery() {
        return searchQuery.get() == null ? "" : searchQuery.get().trim().toLowerCase();
    }

    // -----------------------------
    // ОТСЛЕЖИВАНИЕ ИЗМЕНЕНИЙ
    // -----------------------------

    /**
     * Подключает слушатели изменений.
     * Если меняется заголовок или список блоков — страница становится dirty.
     */
    private void observeChanges() {
        pageTitle.addListener((obs, oldValue, newValue) -> markDirty());

        blocks.addListener((ListChangeListener<BlockDto>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(this::attachBlockListeners);
                }

                if ((change.wasAdded() || change.wasRemoved()) && !suppressDirtyTracking) {
                    markDirty();
                }
            }
        });
    }

    /**
     * Подключает слушатели к конкретному блоку.
     */
    private void attachBlockListeners(BlockDto block) {
        if (!observedBlocks.add(block)) {
            return;
        }

        block.contentProperty().addListener((obs, oldValue, newValue) -> markDirty());
        block.typeProperty().addListener((obs, oldValue, newValue) -> markDirty());
    }

    /**
     * Помечает страницу как изменённую.
     */
    private void markDirty() {
        if (suppressDirtyTracking) {
            return;
        }

        dirty.set(true);
        saveState.set("Unsaved");
        changeVersion.set(changeVersion.get() + 1);
    }

    /**
     * Помечает страницу как сохранённую.
     */
    private void markSaved() {
        dirty.set(false);
        saveState.set("Saved");
    }

    /**
     * Временно отключает dirty-tracking.
     * Используется для загрузки данных из базы или импорта.
     */
    private void runWithoutDirtyTracking(Runnable action) {
        boolean previous = suppressDirtyTracking;
        suppressDirtyTracking = true;

        try {
            action.run();
        } finally {
            suppressDirtyTracking = previous;
        }
    }

    // -----------------------------
    // ВНУТРЕННЯЯ МОДЕЛЬ ДЛЯ ИМПОРТА
    // -----------------------------

    /**
     * Временная структура:
     * content = текст блока
     * localTargets = номера блоков, на которые есть ссылки
     */
    private record ImportedBlock(String content, List<Integer> localTargets) {
    }
}
