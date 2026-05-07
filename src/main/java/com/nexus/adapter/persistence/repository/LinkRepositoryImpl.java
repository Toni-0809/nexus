package com.nexus.adapter.persistence.repository;

import com.nexus.database.DatabaseManager;
import com.nexus.model.Link;
import com.nexus.usecase.port.LinkRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LinkRepositoryImpl implements LinkRepository {
    private final Connection connection = DatabaseManager.getInstance().getConnection();

    @Override
    public Link save(Link link) {
        String sql = "INSERT INTO links(source_block_id, target_block_id) VALUES (?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, link.getSourceBlockId());
            statement.setLong(2, link.getTargetBlockId());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    link.setId(keys.getLong(1));
                }
            }

            return link;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to create link", e);
        }
    }

    @Override
    public List<Link> findBacklinks(long targetBlockId) {
        String sql = """
                SELECT id, source_block_id, target_block_id
                FROM links
                WHERE target_block_id = ?
                ORDER BY id DESC
                """;

        List<Link> result = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, targetBlockId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new Link(
                            rs.getLong("id"),
                            rs.getLong("source_block_id"),
                            rs.getLong("target_block_id")
                    ));
                }
            }

            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load backlinks", e);
        }
    }

    @Override
    public List<Link> findOutgoingLinks(long sourceBlockId) {
        String sql = """
            SELECT id, source_block_id, target_block_id
            FROM links
            WHERE source_block_id = ?
            ORDER BY id ASC
            """;

        List<Link> result = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, sourceBlockId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(new Link(
                            rs.getLong("id"),
                            rs.getLong("source_block_id"),
                            rs.getLong("target_block_id")
                    ));
                }
            }

            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load outgoing links", e);
        }
    }

    @Override
    public boolean exists(long sourceBlockId, long targetBlockId) {
        String sql = """
                SELECT 1
                FROM links
                WHERE source_block_id = ?
                  AND target_block_id = ?
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, sourceBlockId);
            statement.setLong(2, targetBlockId);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to check link existence", e);
        }
    }
}