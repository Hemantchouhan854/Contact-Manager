
package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails.Standalone;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepo;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;
import com.smart.helper.SessionHelper;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepo contactRepo;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private SessionHelper sessionHelper;

	// Common handler
	@ModelAttribute
	public void commonhandler(Model model, Principal principal) {

		String username = principal.getName();
		User user = this.userRepository.getUserByUserName(username);
		model.addAttribute("user", user);

	}

	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		User user = this.userRepository.getUserByUserName(principal.getName());
		this.sessionHelper.removeMessageFromSession();
		model.addAttribute("user", user);
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
	}

	// Controller for the add contact
	@GetMapping("/add-contact")
	public String openAddContact(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());

		return "normal/add_contact_form";
	}

	// Processing add contact form
	@PostMapping("/process-contact")
	public String processcontact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session, Model model) {
		try {
			String username = principal.getName();
			User user = this.userRepository.getUserByUserName(username);

			/*
			 * if(3>2) { throw new Exception(); }
			 */

			// Processing the image upload

			if (file.isEmpty()) {
				System.out.println("Empty file");
				contact.setImage("default.jpg");

			} else {
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img/").getFile();
				System.out.println(">>>>>>>>" + saveFile);
				Path path = Paths.get(saveFile.getAbsolutePath() + "/" + file.getOriginalFilename());
				System.out.println(">>>>>>>>>>>>>>>>>>>>>>" + path);

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("Image uploaded");

			}

			contact.setUser(user);
			user.getContacts().add(contact);
			this.userRepository.save(user);

			// Message on the successful login
			session.setAttribute("message", new Message("Contact Added successfully!!!","success"));
			System.out.println("Data" + contact);
		} catch (Exception e) {
			System.out.println("Erroe: " + e.getMessage());
			e.printStackTrace();
			session.setAttribute("message", new Message("Something went worng!!!","danger"));
		}

		return "normal/add_contact_form";

	}

	// Method for the show contact
	// Also adding pagination from this handler
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal) {

		// Here we are providing the page number and number of contact per page
		// Taking the page number as path variable form the user
		Pageable pageable = PageRequest.of(page, 3);

		model.addAttribute("title", "Show-user-Contacts");
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		Page<Contact> contacts = this.contactRepo.findContactByUser(user.getId(), pageable);
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPage", contacts.getTotalPages());
		return "normal/show_contacts";
	}

	// Handler for the particular contact details

	@GetMapping("/{cId}/contact")
	public String showContactDetail(Model model, @PathVariable("cId") Integer cId, Principal principal) {
		try {
			Optional<Contact> contactoptional = this.contactRepo.findById(cId);
			Contact contact = contactoptional.get();
			String loginuser = principal.getName();
			User user = this.userRepository.getUserByUserName(loginuser);
			if (user.getId() == contact.getUser().getId()) {
				model.addAttribute("title", "User Detail");
				model.addAttribute("contact", contact);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "normal/contact_detail";
	}

	// Deleting contact
	@GetMapping("/{cId}/delete")
	public String deleteContact(@PathVariable("cId") Integer cId, HttpSession session) {

		// Deleting the image in the folder
		try {
			Contact contact = this.contactRepo.findById(cId).get();
			String image = contact.getImage();
			File saveFile = new ClassPathResource("static/img").getFile();
			Path path = Paths.get(saveFile.getAbsolutePath() + "/" + image);
			Files.deleteIfExists(path);

			this.contactRepo.deleteById(cId);
			session.setAttribute("message", new Message("Contact deleted succesfully...", "success"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:/user/show-contacts/0";
	}

	// Update contact handler

	@PostMapping("/update-contact/{cId}")
	public String updateContact(@PathVariable("cId") Integer cId, Model model) {
		Contact contact = this.contactRepo.findById(cId).get();
		model.addAttribute("title", "Update-User");
		model.addAttribute("contact", contact);
		return "normal/update_contact";

	}

	// Handler for the process the information of the updated contact

	@PostMapping("/process-update/{cId}")
	public String updateContact(@PathVariable("cId") Integer cId, @ModelAttribute Contact contact, Model model,
			@RequestParam("profileImage") MultipartFile file, Principal principal, HttpSession session) {
		System.out.println(file);

		try {
			if (file.isEmpty()) {
				Contact contact2 = this.contactRepo.findById(cId).get();
				String image = contact2.getImage();
				System.out.println(image);
				contact.setImage(image);
			} else {

				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + "/" + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());

			}
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);
			contact.setUser(user);
			Contact updatedContact = this.contactRepo.save(contact);
			model.addAttribute("contact", updatedContact);
			session.setAttribute("message", new Message("Contact Updated succesfully...", "success"));

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "normal/contact_detail";
	}

	// user profile section

	@GetMapping("/user-profile")
	public String userProfile(Model model, Principal principal) {
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		model.addAttribute("user", user);
		model.addAttribute("title", "User-Profile");
		return "normal/user_profile";

	}

	// Handler for updating the user information

	@PostMapping("/update-user/{id}")
	public String userProfile_update(@PathVariable("id") Integer id, Model model) {
		User user = this.userRepository.findById(id).get();
		model.addAttribute("user", user);
		model.addAttribute("title", "Update-User");
		return "normal/update_user";
	}

	// Handler for the process user update
	@PostMapping("/process-user-update")
	public String processUserUpdate(@ModelAttribute User user, Model model,
			@RequestParam("profileImage") MultipartFile file, HttpSession session) {

		int id = user.getId();
		try {
			if (file.isEmpty()) {
				User user2 = this.userRepository.findById(id).get();
				String image = user2.getImageUrl();
				user.setImageUrl(image);
			} else {

				user.setImageUrl(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath() + "/" + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				user.setImageUrl(file.getOriginalFilename());

			}
			User UpdatedUser = this.userRepository.save(user);
			model.addAttribute("user", UpdatedUser);
			session.setAttribute("message", new Message("Information updated successfully ", "alert-success"));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "normal/update_user";

	}

	// Handler for the setting

	@GetMapping("/setting")
	public String setting(Model model) {
		model.addAttribute("title", "Setting");

		return "normal/user_setting";
	}

	// Process password handler
	@PostMapping("/process-password")
	public String changePassword(Principal principal,@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword, @RequestParam("reEnterPassword") String reEnterPassword,HttpSession session) {
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		String password = user.getPassword();
		String oldEnPassword = this.passwordEncoder.encode(oldPassword);
		String newEnPasword = this.passwordEncoder.encode(newPassword);
		String reEnterEnPassword = this.passwordEncoder.encode(reEnterPassword);
		
		//checking input from the user
		System.out.println("password >>>>>>>>>>>>>>>>>>"+password);
		System.out.println("old password  >>>>>>>>>>>>>>>>>>"+oldEnPassword);
		System.out.println("new password  >>>>>>>>>>>>>>>>>>"+newEnPasword);
		System.out.println("Re-enter password  >>>>>>>>>>>>>>>>>>"+reEnterEnPassword);
		
		boolean matches=false;
		matches = passwordEncoder.matches(newPassword, password);
		
		if(newPassword.isEmpty()) {
			
			session.setAttribute("message", new Message("Password can't be Empty", "danger"));
			 return "normal/user_setting";
		}
	
		
		if(passwordEncoder.matches(oldPassword, password)) {
			if(newPassword.equals(reEnterPassword)&& matches==false ) {
				
				user.setPassword(this.passwordEncoder.encode(newPassword));
				this.userRepository.save(user);
				session.setAttribute("message", new Message("Password changed successfully", "success"));
				
			}else if(matches==true){
				session.setAttribute("message", new Message("Old Password can't be a new password", "danger"));
				
			}
			
			else{
				session.setAttribute("message", new Message("New Password did not match!!!","danger"));
				return "normal/user_setting";
			}
		}else {
			session.setAttribute("message", new Message("Old Password did't match!!!", "danger"));
		}
		
		
		return "normal/user_setting";
	}
	
	//Delete user handler
	
	@GetMapping("/delete-user/{id}")
	public String deleteUser(@PathVariable("id")Integer id,HttpSession session) {
		
		this.userRepository.deleteById(id);
		session.setAttribute("message", new Message("User deleted successfully", "success"));
		
		return "/signup";
		
		
	}
	
	//handler for verify the user to delete the account
	@PostMapping("/verify")
	public String verify(Principal principal,@RequestParam("password")String password,Model model,HttpSession session) {
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		int id = user.getId();
		String databasePassword = user.getPassword();
		if(passwordEncoder.matches(password, databasePassword)) {
			model.addAttribute("permission",true);
			model.addAttribute("id",id);
			return "normal/user_setting";	
		}
		
		model.addAttribute("permission",false);
		session.setAttribute("message", new Message("Worng Password!!!", "danger"));
		return "normal/user_setting";
	}

}
