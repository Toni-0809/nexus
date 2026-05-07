package com.nexus.adapter.persistence.repository;

import com.nexus.database.DatabaseManager;
import com.nexus.model.entity.Page;
import com.nexus.usecase.port.PageRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PageRepositoryImpl implements PageRepository {
    private final Connection connection = DatabaseManager.getInstance().getConnection();

    @Override
    public Page save(Page page) {
        if (page.getId() <= 0) {
            String sql = "INSERT INTO pages(title) VALUES (?)";

            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, page.getTitle());
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        page.setId(keys.getLong(1));
                    }
                }

                return page;
            } catch (SQLException e) {
                throw new IllegalStateException("Unable to save page", e);
            }
        }

        String sql = "UPDATE pages SET title = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, page.getTitle());
            statement.setLong(2, page.getId());
            statement.executeUpdate();

            return page;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to update page", e);
        }
    }

    @Override
    public List<Page> findAll() {
        String sql = "SELECT id, title FROM pages ORDER BY id DESC";
        List<Page> result = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                result.add(new Page(
                        rs.getLong("id"),
                        rs.getString("title")
                ));
            }

            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load pages", e);
        }
    }

    @Override
    public Optional<Page> findById(long id) {
        String sql = "SELECT id, title FROM pages WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Page(
                            rs.getLong("id"),
                            rs.getString("title")
                    ));
                }

                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load page", e);
        }
    }

    @Override
    public void deleteById(long id) {
        String sql = "DELETE FROM pages WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to delete page", e);
        }
    }
}