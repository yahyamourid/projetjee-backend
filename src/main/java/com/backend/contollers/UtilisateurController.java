package com.backend.contollers;


import com.backend.entities.Utilisateur;
import com.backend.services.UtilisateurService;
import com.backend.utils.JwtTokenUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UtilisateurController {
    @Autowired
    UtilisateurService utilS;

    @GetMapping("/all")
    public List<Utilisateur> getAll(){
        return utilS.getAll();
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody Utilisateur utilisateur){
        if (utilS.findByEmail(utilisateur.getEmail()) != null)
            return new ResponseEntity<>(Map.of("messae","Utilisateur avec cet email existe deja"), HttpStatus.BAD_REQUEST);
        else{
            String hashPWD = BCrypt.hashpw(utilisateur.getPassword(),BCrypt.gensalt());
            utilisateur.setPassword(hashPWD);
            return ResponseEntity.ok(utilS.create(utilisateur));
        }
    }

    @GetMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Utilisateur loginutilisateur){
        Utilisateur utilisateur = utilS.findByEmail(loginutilisateur.getEmail());
        if (utilisateur == null)
            return new ResponseEntity<>(Map.of("message","email ou mot de passe invalide"),HttpStatus.NOT_FOUND);
        else {
            boolean isRegister = BCrypt.checkpw(loginutilisateur.getPassword(), utilisateur.getPassword());
            if(! isRegister)
                return new ResponseEntity<>(Map.of("message","email ou mot de passe invalide"),HttpStatus.NOT_FOUND);
            else{
                String token = JwtTokenUtil.generateToken(utilisateur.getId()+utilisateur.getUserName());
                Map<String, Object> response = Map.of(
                        "message", "Authentification réussie",
                        "user", utilisateur,
                        "token", token
                );
                return ResponseEntity.ok(response);
            }
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Object> delete(@RequestBody  Map<String, String> requestBody, @PathVariable long id){
        String token = requestBody.get("token");
        Utilisateur utilisateur = utilS.findById(id);
        if(utilisateur == null)
            return new ResponseEntity<>(Map.of("message","Utilisateur n'existe pas"), HttpStatus.NOT_FOUND);
        else if(token == null)
            return new ResponseEntity<>(Map.of("message","token est null"),HttpStatus.UNAUTHORIZED);
        else if(!JwtTokenUtil.validateToken(token,utilisateur.getId()+utilisateur.getUserName()))
            return new ResponseEntity<>(Map.of("message","invalide token"),HttpStatus.UNAUTHORIZED);
        else{
            utilS.delete(utilisateur);
            return new ResponseEntity<>(Map.of("message", "utilisateur supprimé avec succès"), HttpStatus.OK);
        }

    }
}
