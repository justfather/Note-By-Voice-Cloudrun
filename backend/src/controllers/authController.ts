import { Request, Response, NextFunction } from 'express';
import bcrypt from 'bcrypt';
import { generateToken } from '../middleware/auth';
import { AppError } from '../middleware/errorHandler';

// This is a simple in-memory store for demo purposes
// In production, use a proper database (PostgreSQL, MongoDB, etc.)
const users = new Map<string, {
  id: string;
  email: string;
  passwordHash: string;
  isPremium: boolean;
  createdAt: Date;
}>();

export const register = async (
  req: Request,
  res: Response,
  next: NextFunction
) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      throw new AppError('Email and password are required', 400);
    }

    // Check if user already exists
    const existingUser = Array.from(users.values()).find(u => u.email === email);
    if (existingUser) {
      throw new AppError('User already exists', 409);
    }

    // Hash password
    const saltRounds = 10;
    const passwordHash = await bcrypt.hash(password, saltRounds);

    // Create user
    const userId = `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    const newUser = {
      id: userId,
      email,
      passwordHash,
      isPremium: false,
      createdAt: new Date(),
    };

    users.set(userId, newUser);

    // Generate token
    const token = generateToken(userId, email);

    res.status(201).json({
      status: 'success',
      data: {
        token,
        user: {
          id: userId,
          email,
          isPremium: false,
        },
      },
    });
  } catch (error) {
    next(error);
  }
};

export const login = async (
  req: Request,
  res: Response,
  next: NextFunction
) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      throw new AppError('Email and password are required', 400);
    }

    // Find user by email
    const user = Array.from(users.values()).find(u => u.email === email);
    if (!user) {
      throw new AppError('Invalid credentials', 401);
    }

    // Verify password
    const isPasswordValid = await bcrypt.compare(password, user.passwordHash);
    if (!isPasswordValid) {
      throw new AppError('Invalid credentials', 401);
    }

    // Generate token
    const token = generateToken(user.id, user.email);

    res.json({
      status: 'success',
      data: {
        token,
        user: {
          id: user.id,
          email: user.email,
          isPremium: user.isPremium,
        },
      },
    });
  } catch (error) {
    next(error);
  }
};

export const refreshToken = async (
  req: Request,
  res: Response,
  next: NextFunction
) => {
  try {
    // In a real implementation, you would validate the refresh token
    // For now, we'll just generate a new access token based on the user ID
    const { userId, email } = req.body;

    if (!userId || !email) {
      throw new AppError('User ID and email are required', 400);
    }

    const user = users.get(userId);
    if (!user || user.email !== email) {
      throw new AppError('Invalid user', 401);
    }

    const token = generateToken(userId, email);

    res.json({
      status: 'success',
      data: { token },
    });
  } catch (error) {
    next(error);
  }
};