package com.backend.contollers;


import com.backend.entities.Utilisateur;
import com.backend.services.UtilisateurService;
import com.backend.utils.EmailSender;
import com.backend.utils.JwtTokenUtil;
import com.backend.utils.ResetPassword;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UtilisateurController {
    @Autowired
    UtilisateurService utilS;
    @Autowired
    EmailSender emailSender;

    @GetMapping("/all")
    public List<Utilisateur> getAll(){
        return utilS.getAll();
    }

    @PostMapping("/register")
    public ResponseEntity<Object> registerProf(@RequestBody Utilisateur utilisateur){
        if (utilS.findByEmail(utilisateur.getEmail()) != null)
            return new ResponseEntity<>(Map.of("message","Utilisateur avec cet email existe deja"), HttpStatus.BAD_REQUEST);
        else{
            String hashPWD = BCrypt.hashpw(utilisateur.getPassword(),BCrypt.gensalt());
            utilisateur.setPassword(hashPWD);
            utilisateur.setActivate(!utilisateur.getRole().equals("prof"));
            return ResponseEntity.ok(utilS.create(utilisateur));
        }
    }

    @GetMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Utilisateur loginutilisateur){
        Utilisateur utilisateur = utilS.findByEmail(loginutilisateur.getEmail());
        if (utilisateur == null)
            return new ResponseEntity<>(Map.of("message","email ou mot de passe invalide"),HttpStatus.NOT_FOUND);
        else if(!utilisateur.isActivate())
            return new ResponseEntity<>(Map.of("message","votre comte est n'est pas active"),HttpStatus.NOT_FOUND);
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

    @PostMapping("/activate/{id}")
    public ResponseEntity<Object> activateCompte(@PathVariable long id){
        Utilisateur utilisateur = utilS.findById(id);
        if(utilisateur == null)
            return new ResponseEntity<>(Map.of("message","Utilisateur n'existe pas"), HttpStatus.NOT_FOUND);
        else {
            if(utilisateur.isActivate()) {
                utilisateur.setActivate(false);
                utilS.update(utilisateur);
                return new ResponseEntity<>(Map.of("message", "Utilisateur est desactive avec succès "), HttpStatus.OK);
            }
            else{
                utilisateur.setActivate(true);
                utilS.update(utilisateur);
                emailSender.sendEmail(utilisateur.getEmail(),"Activation de compte","Votre compte est active avec succès ");
                return new ResponseEntity<>(Map.of("message", "Utilisateur est active avec succès "), HttpStatus.OK);
            }
        }
    }

    @PostMapping("/codeactivation")
    public ResponseEntity<Object> sendCodeActivation(@RequestBody  Map<String, String> requestBody){
        String email = requestBody.get("email");
        Utilisateur utilisateur = utilS.findByEmail(email);
        if(utilisateur == null)
            return new ResponseEntity<>(Map.of("message","Utilisateur n'existe pas"), HttpStatus.NOT_FOUND);
        else{
            String codeActivation = ResetPassword.generateRandomCode();
            String codeActivationhash = BCrypt.hashpw(codeActivation,BCrypt.gensalt());
            utilisateur.setCodeActivation(codeActivationhash);
            utilisateur.setCodeActivationDate(LocalDateTime.now());
            utilS.update(utilisateur);
            emailSender.sendEmail(utilisateur.getEmail(),"Reintilisation de mot de passe","code d'activation \n"+codeActivation);
            return new ResponseEntity<>(Map.of("message", "code d'activation est envoye "), HttpStatus.OK);

        }
    }

    @PostMapping("/resetpwd")
    public ResponseEntity<Object> resetpassword(@RequestBody Map<String,String> requestBody){
        String email = requestBody.get("email");
        String code = requestBody.get("code");
        String newpwd = requestBody.get("password");
        Utilisateur utilisateur = utilS.findByEmail(email);

        if(utilisateur == null)
            return new ResponseEntity<>(Map.of("message","Utilisateur n'existe pas"), HttpStatus.NOT_FOUND);
        else{
            if (code == null)
                return new ResponseEntity<>(Map.of("message","code est vide"), HttpStatus.NOT_FOUND);
            else {
                if(!BCrypt.checkpw(code,utilisateur.getCodeActivation()))
                    return new ResponseEntity<>(Map.of("message","code d'activation est invalide"), HttpStatus.NOT_FOUND);
                else if (!ResetPassword.isResetCodeValid(utilisateur))
                    return new ResponseEntity<>(Map.of("message","code d'activation est expire"), HttpStatus.NOT_FOUND);

                else{
                    utilisateur.setPassword(BCrypt.hashpw(newpwd,BCrypt.gensalt()));
                    utilS.update(utilisateur);
                    return new ResponseEntity<>(Map.of("message","mot de passe est modifie avec succes"), HttpStatus.OK);
                }
            }
        }

    }
}
