#pragma once

#include <rain.hpp>

namespace Xena {
	class PointLl {
		public:
		long long x, y;

		bool operator==(PointLl const &);
	};
}

template <>
struct std::hash<Xena::PointLl> {
	std::size_t operator()(Xena::PointLl const &) const noexcept;
};
