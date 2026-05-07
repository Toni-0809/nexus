package com.nexus;

// Реализации репозиториев (работа с SQLite)
import com.nexus.adapter.persistence.repository.BlockRepositoryImpl;
import com.nexus.adapter.persistence.repository.LinkRepositoryImpl;
import com.nexus.adapter.persistence.repository.PageRepositoryImpl;

// Главная ViewModel экрана
import com.nexus.adapter.ui.viewmodel.MainViewModel;

// Use cases приложения
import com.nexus.usecase.CreateLinkUseCase;
import com.nexus.usecase.CreatePageUseCase;
import com.nexus.usecase.GetBacklinksForBlockUseCase;
import com.nexus.usecase.GetBlocksForPageUseCase;
import com.nexus.usecase.GetOutgoingLinksForBlockUseCase;

// Интерфейсы репозиториев
import com.nexus.usecase.port.BlockRepository;
import com.nexus.usecase.port.LinkRepository;
import com.nexus.usecase.port.PageRepository;

/**
 * ApplicationConfig — это класс, который вручную собирает зависимости приложения.
 *
 * Он нужен для того, чтобы:
 * - создать репозитории,
 * - создать use cases,
 * - собрать готовую MainViewModel.
 *
 * Проще говоря:
 * это "точка сборки" приложения.
 */
public final class ApplicationConfig {

    private ApplicationConfig() {
    }

    /**
     * Создаёт и возвращает полностью собранную MainViewModel.
     */
    public static MainViewModel createMainViewModel() {

        // ---------------------------------
        // 1. СОЗДАЁМ РЕПОЗИТОРИИ
        // ---------------------------------
        // Это адаптеры, которые реально работают с базой данных SQLite.

        PageRepository pageRepository = new PageRepositoryImpl();
        BlockRepository blockRepository = new BlockRepositoryImpl();
        LinkRepository linkRepository = new LinkRepositoryImpl();

        // ---------------------------------
        // 2. СОЗДАЁМ USE CASE
        // ---------------------------------
        // Каждый use case получает только те зависимости, которые ему нужны.

        // Создание новой страницы
        CreatePageUseCase createPageUseCase = new CreatePageUseCase(pageRepository);

        // Загрузка блоков для страницы
        GetBlocksForPageUseCase getBlocksForPageUseCase = new GetBlocksForPageUseCase(blockRepository);

        // Создание ссылки между двумя блоками
        CreateLinkUseCase createLinkUseCase = new CreateLinkUseCase(linkRepository);

        // Получение всех обратных ссылок на блок
        GetBacklinksForBlockUseCase getBacklinksForBlockUseCase = new GetBacklinksForBlockUseCase(linkRepository);

        // Получение исходящих ссылок из блока
        GetOutgoingLinksForBlockUseCase getOutgoingLinksForBlockUseCase =
                new GetOutgoingLinksForBlockUseCase(linkRepository);

        // ---------------------------------
        // 3. СОЗДАЁМ VIEWMODEL
        // ---------------------------------
        // Передаём в неё все use cases и нужные репозитории.

        return new MainViewModel(
                createPageUseCase,
                getBlocksForPageUseCase,
                createLinkUseCase,
                getBacklinksForBlockUseCase,
                getOutgoingLinksForBlockUseCase,
                pageRepository,
                blockRepository
        );
    }
}