package com.nexus.adapter.persistence.repository;

import com.nexus.database.DatabaseManager;
import com.nexus.model.entity.Block;
import com.nexus.usecase.port.BlockRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BlockRepositoryImpl implements BlockRepository {
    private final Connection connection = DatabaseManager.getInstance().getConnection();

    @Override
    public Block save(Block block) {
        if (block.getId() <= 0) {
            String sql = "INSERT INTO blocks(page_id, type, content, order_index) VALUES (?, ?, ?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, block.getPageId());
                statement.setString(2, block.getType().name());
                statement.setString(3, block.getContent());
                statement.setInt(4, block.getOrderIndex());
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        block.setId(keys.getLong(1));
                    }
                }

                return block;
            } catch (SQLException e) {
                throw new IllegalStateException("Unable to insert block", e);
            }
        }

        String sql = "UPDATE blocks SET page_id = ?, type = ?, content = ?, order_index = ? WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, block.getPageId());
            statement.setString(2, block.getType().name());
            statement.setString(3, block.getContent());
            statement.setInt(4, block.getOrderIndex());
            statement.setLong(5, block.getId());
            statement.executeUpdate();

            return block;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to update block", e);
        }
    }

    @Override
    public List<Block> findByPageId(long pageId) {
        String sql = """
                SELECT id, page_id, type, content, order_index
                FROM blocks
                WHERE page_id = ?
                ORDER BY order_index ASC, id ASC
                """;

        List<Block> result = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pageId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }

            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load blocks", e);
        }
    }

    @Override
    public Optional<Block> findById(long blockId) {
        String sql = """
                SELECT id, page_id, type, content, order_index
                FROM blocks
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, blockId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }

                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load block", e);
        }
    }

    private Block map(ResultSet rs) throws SQLException {
        return new Block(
                rs.getLong("id"),
                rs.getLong("page_id"),
                Block.BlockType.valueOf(rs.getString("type")),
                rs.getString("content"),
                rs.getInt("order_index")
        );
    }

    @Override
    public void deleteById(long blockId) {
        String sql = "DELETE FROM blocks WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, blockId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to delete block", e);
        }
    }
}