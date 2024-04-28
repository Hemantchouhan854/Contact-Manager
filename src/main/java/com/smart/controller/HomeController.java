package com.smart.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class HomeController {
	
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private UserRepository userRepository;


	@RequestMapping("/")
	public String home(Model m) {

		m.addAttribute("title", "Home - Smart Contact Manager");
		return "home";
	}

	@RequestMapping("/about")
	public String about(Model m) {

		m.addAttribute("title", "About - Smart Contact Manager");
		return "about";
	}

	@RequestMapping("/signup")
	public String signup(Model m) {

		m.addAttribute("title", "Signup - Smart Contact Manager");
		User user = new User();
		m.addAttribute("user", user);
		return "signup";
	}

	@PostMapping("/register")
	public String registerUser(@Valid @ModelAttribute("user") User user,BindingResult result  ,

			@RequestParam(value = "agreement", defaultValue = "false") boolean agreement, Model model,
			HttpSession session) {

		try {
			if (!agreement) {
				System.out.println("You have not agreed the terms and conditions");
				throw new Exception("You have not agreed the terms and conditions");
			}
			
			if(result.hasErrors()) {
				System.out.println("Error"+ result.toString());
				model.addAttribute("user",user);
				
				return "signup";
			}

			user.setRole("ROLE_USER");
			user.setEnabled(true);
			user.setImageUrl("default.jpg");
			user.setPassword(passwordEncoder.encode(user.getPassword()));

			userRepository.save(user);
			model.addAttribute("user", new User());
			session.setAttribute("message", new Message("Register successfully!!!","success"));

			
			return "signup";

		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("user", user);
			session.setAttribute("message", new Message("Something Went wrong!!" + e.getMessage(), "alert-danger"));
			return "signup";
		}
		
	}

	//Handler for custom login
	
	@GetMapping("/signin")
	public String customLogin(Model model) {
		model.addAttribute("title", "Login - Smart Contact Manager");
		return "login";
	}
}
