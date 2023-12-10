package com.backend.services;

import com.backend.dao.IDao;
import com.backend.entities.Utilisateur;
import com.backend.repositories.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class UtilisateurService implements IDao<Utilisateur> {

    @Autowired
    UtilisateurRepository utiliR;

    @Override
    public List<Utilisateur> getAll() {
        return utiliR.findAll();
    }

    @Override
    public Utilisateur findById(long id) {
        return utiliR.findById(id).orElse(null);
    }

    @Override
    public Utilisateur create(Utilisateur o) {
        return utiliR.save(o);
    }

    @Override
    public Utilisateur update(Utilisateur o) {
        return utiliR.save(o);
    }

    @Override
    public boolean delete(Utilisateur o) {
        try {
            utiliR.delete(o);
            return true;
        }catch (Exception e) {
            return false;
        }
    }

    public Utilisateur findByEmail(String email){
        return utiliR.findUtilisateurByEmail(email);
    }
}
