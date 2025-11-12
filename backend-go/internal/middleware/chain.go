package middleware

import (
	"net/http"
)

// Middleware represents an HTTP middleware function
type Middleware func(http.Handler) http.Handler

// Chain represents a chain of middlewares
type Chain struct {
	middlewares []Middleware
}

// NewChain creates a new middleware chain
func NewChain(middlewares ...Middleware) *Chain {
	return &Chain{
		middlewares: middlewares,
	}
}

// Then chains middlewares and wraps the final handler
func (c *Chain) Then(h http.Handler) http.Handler {
	// Apply middlewares in reverse order
	for i := len(c.middlewares) - 1; i >= 0; i-- {
		h = c.middlewares[i](h)
	}
	return h
}
