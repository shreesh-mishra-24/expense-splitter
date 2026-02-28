package com.expensesplitter.repository;

import com.expensesplitter.model.Group;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for storing groups.
 *
 * This implementation uses a thread-safe ConcurrentHashMap for storage.
 * Can be easily replaced with a database-backed implementation.
 */
@Repository
public class GroupRepository {

    private final Map<UUID, Group> groups = new ConcurrentHashMap<>();

    /**
     * Saves a group to the repository.
     *
     * @param group the group to save
     * @return the saved group
     */
    public Group save(Group group) {
        groups.put(group.getId(), group);
        return group;
    }

    /**
     * Finds a group by its ID.
     *
     * @param id the ID of the group
     * @return Optional containing the group if found
     */
    public Optional<Group> findById(UUID id) {
        return Optional.ofNullable(groups.get(id));
    }

    /**
     * Returns all groups.
     *
     * @return list of all groups
     */
    public List<Group> findAll() {
        return new ArrayList<>(groups.values());
    }

    /**
     * Deletes a group by its ID.
     *
     * @param id the ID of the group to delete
     * @return true if the group was deleted
     */
    public boolean deleteById(UUID id) {
        return groups.remove(id) != null;
    }

    /**
     * Checks if a group exists by ID.
     *
     * @param id the ID to check
     * @return true if the group exists
     */
    public boolean existsById(UUID id) {
        return groups.containsKey(id);
    }

    /**
     * Clears all groups from the repository.
     * Useful for testing purposes.
     */
    public void clear() {
        groups.clear();
    }
}
